package com.ncores.plaluvs.controller.cometic;

import com.ncores.plaluvs.controller.cometic.dto.CosmeticElementsResponseDto;
import com.ncores.plaluvs.controller.cometic.dto.DetailCosmeticDto;
import com.ncores.plaluvs.controller.cometic.dto.SimpleCosmeticDto;
import com.ncores.plaluvs.crawling.CrawlingItemDto;
import com.ncores.plaluvs.crawling.ReadJsonFile;
import com.ncores.plaluvs.domain.Elements;
import com.ncores.plaluvs.domain.dto.PagingResponseDto;
import com.ncores.plaluvs.domain.dto.PagingSimpleResponseDto;
import com.ncores.plaluvs.domain.user.User;
import com.ncores.plaluvs.exception.ErrorCode;
import com.ncores.plaluvs.exception.PlaluvsException;
import com.ncores.plaluvs.repository.ElementsRepository;
import com.ncores.plaluvs.security.UserDetailsImpl;
import com.ncores.plaluvs.service.CosmeticService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.parser.ParseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
public class CosmeticController {
    private final ReadJsonFile readJsonFile;
    private final CosmeticService cosmeticService;

    //수정할 것
    private final ElementsRepository elementsRepository;


    @GetMapping("/item/save")
    public void itemSave() throws PlaluvsException, IOException, ParseException {
        List<CrawlingItemDto> crawlingItemDtoList = readJsonFile.readJsonFile();

        log.info("crawlingItemDtoList = {}", crawlingItemDtoList.get(0).getName());
        log.info("size = {}", crawlingItemDtoList.size());

        readJsonFile.saveJsonFile();
    }


    @GetMapping("/cosmetic/simple-recommends")
    public ResponseEntity<?> cosmeticSimpleRecommends(@AuthenticationPrincipal UserDetailsImpl userDetails){

        List<SimpleCosmeticDto> result = cosmeticService.cosmeticSimpleRecommends(userDetails);

        return new ResponseEntity<>(new PagingSimpleResponseDto(result.size(), result), HttpStatus.OK);
    }

    @GetMapping("/cosmetic/detail-recommends/{category}/{page}")
    public ResponseEntity<?> cosmeticDetailRecommends(
            @PathVariable("category") Long categoryId,
            @PathVariable Long page,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) throws PlaluvsException {

        UserDetailsImpl.UserCheck(userDetails);
        List<DetailCosmeticDto> result = cosmeticService.cosmeticDetailRecommends(userDetails, categoryId, page);

        return new ResponseEntity<>(new PagingResponseDto(result.size(), page.intValue(), 4, result), HttpStatus.OK);
    }

    @GetMapping("/cosmetic/elements-recommend/{elements}/{category}/{page}")
    public ResponseEntity<?> cosmeticElements(@PathVariable(value = "elements") Long elementsId,@PathVariable("category") Long categoryId, @PathVariable Long page) throws PlaluvsException {

        List<DetailCosmeticDto> result = cosmeticService.cosmeticContainsElements(elementsId, categoryId, page);

        Elements elements = elementsRepository.findById(elementsId).orElseThrow(
                () -> new PlaluvsException(ErrorCode.ELEMENT_NOT_FOUND)
        );


        return new ResponseEntity<>(new CosmeticElementsResponseDto(result.size(), page, 4L, elements.getKorean(), result), HttpStatus.OK);
    }

    @GetMapping("/cosmetic/worry-recommends")
    public ResponseEntity<?> cosmeticWorry(@AuthenticationPrincipal UserDetailsImpl userDetails) throws PlaluvsException {
        List<SimpleCosmeticDto> result = cosmeticService.cosmeticWorry(userDetails);

        return new ResponseEntity<>(new PagingSimpleResponseDto(result.size(), result), HttpStatus.OK);
    }

}
