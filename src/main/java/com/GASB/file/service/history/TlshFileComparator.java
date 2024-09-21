package com.GASB.file.service.history;

import com.GASB.file.exception.FileComparisonException;
import com.GASB.file.model.entity.Activities;
import com.GASB.file.service.tlsh.Tlsh;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TlshFileComparator {

    private static final int MAX_DIFF = 1000;

    public double compareFiles(Activities act, Activities cmpAct) {
        if (act.getId().equals(cmpAct.getId())) {
            return 100;
        }

        String tlsh1Str = act.getTlsh();
        String tlsh2Str = cmpAct.getTlsh();

        try {
            Tlsh hash1 = Tlsh.fromTlshStr(tlsh1Str);
            Tlsh hash2 = Tlsh.fromTlshStr(tlsh2Str);

            // 두 해시 간의 차이 계산
            int diff = hash1.totalDiff(hash2, true);
            log.info("diff: {}", diff);

            // 유사도 퍼센트 계산
            double similarityPercentage = (1 - (double) diff / MAX_DIFF) * 100;
            similarityPercentage = Math.round(similarityPercentage * 100.0) / 100.0;

            log.info("Similarity between files: {}%", similarityPercentage);

            return Math.max(similarityPercentage, 0);
        } catch (RuntimeException e) {
            log.error("Error comparing files: {}", e.getMessage(), e);
            return -999;
        }
    }
}


