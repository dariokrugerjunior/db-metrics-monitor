package br.com.vivovaloriza.dbmetricsmonitor.service;

import br.com.vivovaloriza.dbmetricsmonitor.config.AppProperties;
import br.com.vivovaloriza.dbmetricsmonitor.dto.TerminateSessionRequest;
import br.com.vivovaloriza.dbmetricsmonitor.exception.BadRequestException;
import br.com.vivovaloriza.dbmetricsmonitor.repository.DatabaseMonitoringRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DatabaseMonitoringServiceTest {

    private DatabaseMonitoringRepository repository;
    private DatabaseMonitoringService service;

    @BeforeEach
    void setUp() {
        repository = mock(DatabaseMonitoringRepository.class);
        AppProperties appProperties = new AppProperties();
        service = new DatabaseMonitoringService(repository, appProperties);
        ReflectionTestUtils.setField(service, "applicationName", "db-metrics-monitor");
    }

    @Test
    void shouldTerminateSessionWhenValidAndForced() {
        when(repository.sessionExists(123L)).thenReturn(true);
        when(repository.getCurrentBackendPid()).thenReturn(456L);
        when(repository.findSessionApplicationName(123L)).thenReturn("psql");
        when(repository.terminateSession(123L)).thenReturn(true);

        var response = service.terminateSession(123L, new TerminateSessionRequest("maintenance", true));

        assertThat(response.pid()).isEqualTo(123L);
        assertThat(response.terminated()).isTrue();
        verify(repository).terminateSession(123L);
    }

    @Test
    void shouldBlockTerminationOfOwnBackend() {
        when(repository.sessionExists(456L)).thenReturn(true);
        when(repository.getCurrentBackendPid()).thenReturn(456L);

        assertThatThrownBy(() -> service.terminateSession(456L, new TerminateSessionRequest("unsafe", true)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("backend atual");
    }
}
