package com.GASB.file.service.history;

import com.GASB.file.model.entity.*;
import com.GASB.file.repository.file.ActivitiesRepo;
import com.GASB.file.repository.file.FileGroupRepo;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class FileSimilarService {

    private final ActivitiesRepo activitiesRepo;
    private final FileGroupRepo fileGroupRepo;

    @Autowired
    public FileSimilarService(ActivitiesRepo activitiesRepo, FileGroupRepo fileGroupRepo) {
        this.activitiesRepo = activitiesRepo;
        this.fileGroupRepo = fileGroupRepo;
    }

    // 유사도 측정
    private double calculateSim(String a, String b) {
        JaroWinklerSimilarity similarity = new JaroWinklerSimilarity();
        return similarity.apply(a, b);
    }

    // 파일네임에서 확장자 제거
    private String noExtesion(String fileName) {
        return FilenameUtils.getBaseName(fileName).toLowerCase();  // Convert to lower case for consistency
    }

    public double getFileSimilarity(Long actId, Long cmpId) {

        // 1. actId로 activities 객체 조회
        Optional<Activities> activity = activitiesRepo.findById(actId);
        Optional<Activities> cmpAct = activitiesRepo.findById(cmpId);
        if (activity.isEmpty() || cmpAct.isEmpty()) {
            System.out.println("Not found for ID");
            return 404;
        }

        // 2. actId의 그룹에 cmpId가 속해있는지
        Optional<FileGroup> actGroupOpt = fileGroupRepo.findById(actId);
        Optional<FileGroup> cmpGroupOpt = fileGroupRepo.findById(cmpId);
        // Optional에서 그룹 이름 추출
        String actGroupName = actGroupOpt.map(FileGroup::getGroupName).orElse("Unknown");
        String cmpGroupName = cmpGroupOpt.map(FileGroup::getGroupName).orElse("Unknown");


        if (actGroupOpt.isEmpty() || cmpGroupOpt.isEmpty()) {
            System.out.println("Not Found Group for ID");
            return 505;
        }

        // 그룹 이름이 일치하지 않는 경우
        if (!actGroupName.equals(cmpGroupName)) {
            System.out.println("Group Name Mismatch");
            return 606;
        }

        // 3. act와 cmp의 fileName (확장자 제거한 이름)
        String actFileName = noExtesion(activity.get().getFileName());
        String cmpFileName = noExtesion(cmpAct.get().getFileName());

        // 4. actFileName과 cmpFileName의 유사도 측정
        double similarity = calculateSim(actFileName, cmpFileName);

        return similarity;
    }
}
