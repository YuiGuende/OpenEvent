package com.group02.openevent.ai.qdrant.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Runner chạy các tác vụ đồng bộ hóa vector khi ứng dụng khởi động.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VectorSeedRunner implements CommandLineRunner {

    // Inject service đồng bộ hóa đã hoàn chỉnh
    private final EventVectorSyncService syncService;
    private final QdrantService qdrantService;
    // ⚡ BỔ SUNG: Tiêm cờ điều khiển
    @Value("${ai.qdrant.run-full-seed}")
    private boolean runFullSeed;

    @Override
    public void run(String... args) throws Exception {
        if (runFullSeed) {
            log.warn("RUNNING FULL VECTOR SEEDING. THIS WILL CONSUME API CREDITS.");
            log.info("Starting Vector Synchronization and Seed process...");

            try {
                qdrantService.createPayloadIndex("kind", "keyword");
                log.info("✅ Payload Index for 'kind' created successfully.");
            } catch (Exception e) {
                log.error("❌ Failed to create 'kind' index: {}", e.getMessage());
            }

            // 1. Đồng bộ hóa Sự kiện
            try {
                syncService.syncAllEvents();
                log.info("✅ Event vectors synced successfully.");
            } catch (Exception e) {
                log.error("❌ Critical error during Event sync: {}", e.getMessage(), e);
            }

            // 2. Đồng bộ hóa Địa điểm
            try {
                syncService.seedAllPlaces();
                log.info("✅ Place vectors seeded successfully.");
            } catch (Exception e) {
                log.error("❌ Critical error during Place seed: {}", e.getMessage(), e);
            }

            // 3. Seed các Intent mẫu
            try {
                syncService.seedPromptSummary();
                syncService.seedPromptSendEmail();
                syncService.seedOutdoorActivities(); // Hoặc tên hàm mới: seedOutdoorEventLabels
                syncService.seedPromptAddEvent();
                log.info("✅ Intent seed data loaded successfully.");
            } catch (Exception e) {
                log.error("❌ Critical error during Intent seed: {}", e.getMessage(), e);
            }

        } else {
            log.info("Skipping vector seeding. Run 'ai.qdrant.run-full-seed=true' to enable.");
        }

        log.info("Vector seeding process finished.");
    }
}