package com.lee.osakacity.service;

import com.lee.osakacity.dto.Category;
import com.lee.osakacity.dto.GuideResponseDto;
import com.lee.osakacity.dto.GuideSimpleResponse;
import com.lee.osakacity.infra.entity.Guide;
import com.lee.osakacity.infra.repository.GuideRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GuideService {
    private final GuideRepo guideRepo;
    public List<GuideSimpleResponse> getMoreGuide(Category category) {
        List<Guide> entityLIst;
        if ( category.equals(Category.all) )
            entityLIst = guideRepo.findAllByOrderByIdAsc();
         else
            entityLIst = guideRepo.findByCategoryOrderByIdAsc(category);

        return entityLIst.stream().map(GuideSimpleResponse::new).toList();
    }

    public GuideResponseDto getDetailPage(Long id) {
        Guide guide = guideRepo.findById(id).orElseThrow(RuntimeException::new);
        return new GuideResponseDto(guide);
    }
}
