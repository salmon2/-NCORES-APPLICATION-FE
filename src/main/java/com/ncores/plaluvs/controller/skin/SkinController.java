package com.ncores.plaluvs.controller.skin;

import com.ncores.plaluvs.controller.skin.dto.*;
import com.ncores.plaluvs.domain.dto.PagingResponseDto;
import com.ncores.plaluvs.domain.dto.SkinNowStatusRequestDto;
import com.ncores.plaluvs.domain.dto.SkinWorryRequestDto;
import com.ncores.plaluvs.exception.PlaluvsException;
import com.ncores.plaluvs.security.UserDetailsImpl;
import com.ncores.plaluvs.service.SkinService;
import com.ncores.plaluvs.service.SkinTypeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
public class SkinController {
    private final SkinService skinService;
    private final SkinTypeService skinTypeService;


    @PostMapping("/skin/now/status")
    public ResponseEntity<?> skinOilIndicate(@RequestBody SkinNowStatusRequestDto requestDto,
                                             @AuthenticationPrincipal UserDetailsImpl userDetails) throws PlaluvsException {
        log.info("/skin/now/status");
        log.info("requestDto = {}", requestDto);

        UserDetailsImpl.UserCheck(userDetails);

        skinService.currentSkinStatus(requestDto, userDetails);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/skin/worry")
    public ResponseEntity<?> skinWorryIndicate(@RequestBody SkinWorryRequestDto requestDto,
                                               @AuthenticationPrincipal UserDetailsImpl userDetails) throws PlaluvsException {

        log.info("/skin/worry");
        log.info("requestDto = {}", requestDto);
        //skinworry 없으면 걸기
        UserDetailsImpl.UserCheck(userDetails);
        skinService.skinWorryUpdate(requestDto, userDetails);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/skin/daily/status")
    public ResponseEntity<?> skinDailyStatus(@RequestBody SkinDailyStatusRequestDto requestDto,
                                               @AuthenticationPrincipal UserDetailsImpl userDetails) throws PlaluvsException {

        log.info("/skin/daily/status");
        log.info("requestDto = {}", requestDto);

        UserDetailsImpl.UserCheck(userDetails);
        skinService.skinDailyStatus(requestDto, userDetails);

        return null;
    }

    @PostMapping("/skin/daily/Stimulation")
    public ResponseEntity<?> skinDailyStimulation(@RequestBody SkinDailyStimulationRequestDto requestDto,
                                             @AuthenticationPrincipal UserDetailsImpl userDetails) throws PlaluvsException {

        log.info("/skin/daily/status");
        log.info("requestDto = {}", requestDto);

        UserDetailsImpl.UserCheck(userDetails);
        skinService.skinDailyStimulation(requestDto, userDetails);

        return null;
    }

    @PostMapping("/skin/daily/self-check")
    public ResponseEntity<?> skinDailySefCheck(@RequestBody SkinDailySefCheckRequestDto requestDto,
                                               @AuthenticationPrincipal UserDetailsImpl userDetails
                                               ) throws PlaluvsException{
        log.info("skin/daily/self-check");
        log.info("requestDto = {}", requestDto);

        UserDetailsImpl.UserCheck(userDetails);
        skinService.skinSelfCheck(requestDto, userDetails);

        String boumanType = skinService.skinBoumanCalucluate(userDetails);
        skinTypeService.findSkinElements(userDetails);


        return new ResponseEntity<>(HttpStatus.OK);
    }


    @GetMapping("/skin/status")
    public ResponseEntity<?> skinStatus(@AuthenticationPrincipal UserDetailsImpl userDetails) throws PlaluvsException {
        log.info("/skin/status");
        SkinStatusResponseDto result = skinService.skinStatus(userDetails);

        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @GetMapping("/skin/status/list/{page}")
    public ResponseEntity<?> skinStatusList(@AuthenticationPrincipal UserDetailsImpl userDetails,
                                            @PathVariable Long page,
                                            @RequestParam(defaultValue = "asc") String sort){
        log.info("skin/status/list");
        PagingAveragingScoreResponseDto result = skinService.skinStatusList(userDetails, page, sort);

        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @GetMapping("/skin/status/bouman")
    public ResponseEntity<?> skinStatusBouman(@AuthenticationPrincipal UserDetailsImpl userDetails) throws PlaluvsException {
        log.info("skin/status/list");

        //skinStatusBoumanResponseDto result = skinService.skinStatusBoumanDummy(userDetails);
        skinStatusBoumanResponseDto result = skinService.skinStatusBouman(userDetails);


        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @GetMapping("/skin/status/record/{page}")
    public ResponseEntity<?> skinStatusRecord(@AuthenticationPrincipal UserDetailsImpl userDetails,
                                              @PathVariable Long page){
        log.info("skin/status/list");
        Page<SkinStatusRecordResponseDto> result = skinService.skinStatusRecord(userDetails, page);

        return new ResponseEntity<>(new PagingResponseDto(result.getContent().size(), result.getNumber(), result.getTotalPages(), result.getContent()), HttpStatus.OK);
    }
}


