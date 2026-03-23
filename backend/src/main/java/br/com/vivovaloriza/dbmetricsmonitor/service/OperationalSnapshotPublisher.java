package br.com.vivovaloriza.dbmetricsmonitor.service;

import br.com.vivovaloriza.dbmetricsmonitor.model.OperationalSnapshot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OperationalSnapshotPublisher {

    public void publish(OperationalSnapshot snapshot) {
        log.info("snapshot_published generatedAt={} status={} totalConnections={} totalLocks={} runningQueries={}",
                snapshot.generatedAt(),
                snapshot.summary().database().status(),
                snapshot.summary().database().connections().totalConnections(),
                snapshot.summary().database().locks().total(),
                snapshot.summary().database().runningQueries().total());
    }
}
