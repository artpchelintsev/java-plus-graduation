package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.client.EventFeignClient;
import ru.practicum.common.EntityValidator;
import ru.practicum.dto.CompilationDto;
import ru.practicum.dto.NewCompilationDto;
import ru.practicum.dto.UpdateCompilationRequest;
import ru.practicum.dto.event.EventBatchDto;
import ru.practicum.exception.ConflictException;
import ru.practicum.mapper.CompilationMapper;
import ru.practicum.model.Compilation;
import ru.practicum.repository.CompilationEventRepository;
import ru.practicum.repository.CompilationRepository;

import java.util.Collections;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class CompilationServiceImpl implements CompilationService {

    private final CompilationRepository compilationRepository;
    private final CompilationEventRepository compilationEventRepository;
    private final CompilationMapper compilationMapper;
    private final EntityValidator entityValidator;
    private final EventFeignClient eventFeignClient;

    @Override
    @Transactional
    public CompilationDto createCompilation(NewCompilationDto compilationDto) {
        log.info("Создание новой подборки с названием: {}", compilationDto.getTitle());

        if (compilationRepository.existsByTitle(compilationDto.getTitle())) {
            throw new ConflictException("Подборка с названием='" + compilationDto.getTitle() + "' уже существует");
        }

        Compilation compilation = compilationMapper.toEntity(compilationDto);
        Compilation savedCompilation = compilationRepository.save(compilation);

        if (compilationDto.getEvents() != null && !compilationDto.getEvents().isEmpty()) {
            saveCompilationEvents(savedCompilation.getId(), compilationDto.getEvents());
        }

        log.info("Подборка создана с id: {}", savedCompilation.getId());
        return getCompilationDtoWithEvents(savedCompilation);
    }

    @Override
    @Transactional
    public CompilationDto updateCompilation(Long compId, UpdateCompilationRequest request) {
        log.info("Обновление подборки с id: {}", compId);

        Compilation compilation = entityValidator.ensureAndGet(
                compilationRepository, compId, "Подборка"
        );

        if (request.getTitle() != null && !request.getTitle().equals(compilation.getTitle())) {
            if (compilationRepository.existsByTitle(request.getTitle())) {
                throw new ConflictException("Подборка с названием='" + request.getTitle() + "' уже существует");
            }
            compilation.setTitle(request.getTitle());
        }

        if (request.getPinned() != null) {
            compilation.setPinned(request.getPinned());
        }

        if (request.getEvents() != null) {
            compilationEventRepository.deleteByCompilationId(compId);
            if (!request.getEvents().isEmpty()) {
                saveCompilationEvents(compId, request.getEvents());
            }
        }

        Compilation updatedCompilation = compilationRepository.save(compilation);
        log.info("Подборка обновлена с id: {}", compId);

        return getCompilationDtoWithEvents(updatedCompilation);
    }

    @Override
    @Transactional
    public void deleteCompilation(Long compId) {
        log.info("Удаление подборки с id: {}", compId);
        entityValidator.ensureExists(compilationRepository, compId, "Подборка");
        compilationRepository.deleteById(compId);
        log.info("Подборка удалена с id: {}", compId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompilationDto> getCompilations(Boolean pinned, Integer from, Integer size) {
        log.info("Получение подборок с pinned={}, from={}, size={}", pinned, from, size);

        Pageable pageable = PageRequest.of(from / size, size);
        Page<Compilation> compilationsPage = compilationRepository.findByPinned(pinned, pageable);

        return compilationsPage.getContent().stream()
                .map(this::getCompilationDtoWithEvents)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CompilationDto getCompilationById(Long compId) {
        log.info("Получение подборки по id: {}", compId);

        Compilation compilation = entityValidator.ensureAndGet(
                compilationRepository, compId, "Подборка"
        );

        return getCompilationDtoWithEvents(compilation);
    }

    private void saveCompilationEvents(Long compilationId, List<Long> eventIds) {
        var compilationEvents = eventIds.stream()
                .map(eventId -> new ru.practicum.model.CompilationEvent(compilationId, eventId))
                .toList();
        compilationEventRepository.saveAll(compilationEvents);
    }

    private CompilationDto getCompilationDtoWithEvents(Compilation compilation) {
        List<Long> eventIds = compilationEventRepository.findEventIdsByCompilationId(compilation.getId());

        CompilationDto dto = compilationMapper.toDto(compilation);

        // Получаем события через FeignClient
        if (!eventIds.isEmpty()) {
            try {
                EventBatchDto batchDto = eventFeignClient.getEventsByIds(eventIds);
                dto.setEvents(batchDto.getEvents().values().stream().toList());
            } catch (Exception e) {
                log.error("Ошибка при получении событий из event-service", e);
                dto.setEvents(Collections.emptyList());
            }
        } else {
            dto.setEvents(Collections.emptyList());
        }

        return dto;
    }
}