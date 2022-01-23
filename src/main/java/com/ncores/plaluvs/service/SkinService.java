package com.ncores.plaluvs.service;

import com.ncores.plaluvs.controller.skin.dto.*;
import com.ncores.plaluvs.domain.*;
import com.ncores.plaluvs.domain.dto.SkinNowStatusRequestDto;
import com.ncores.plaluvs.domain.dto.SkinWorryRequestDto;
import com.ncores.plaluvs.domain.skintype.Bouman;
import com.ncores.plaluvs.domain.skintype.CurrentSkinStatus;
import com.ncores.plaluvs.domain.skintype.skindailyStatus.SkinDailyStatus;
import com.ncores.plaluvs.domain.skintype.skindailyStatus.SkinDailyStatusEnum;
import com.ncores.plaluvs.domain.skintype.skindailystimulation.SkinDailyStimulation;
import com.ncores.plaluvs.domain.skintype.skindailystimulation.SkinDailyStimulationEnum;
import com.ncores.plaluvs.domain.skintype.skintrouble.*;
import com.ncores.plaluvs.domain.skintype.SkinType;
import com.ncores.plaluvs.domain.user.User;
import com.ncores.plaluvs.exception.ErrorCode;
import com.ncores.plaluvs.exception.PlaluvsException;
import com.ncores.plaluvs.repository.*;
import com.ncores.plaluvs.repository.elements.ElementsRepository;
import com.ncores.plaluvs.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class SkinService {
    private final SkinTypeRepository skinTypeRepository;
    private final SKinWorryRepository skinWorryRepository;
    private final SkinDailyStatusRepository skinDailyStatusRepository;
    private final ElementsRepository elementsRepository;
    private final SkinDailyStimulationRepository skinDailyStimulationRepository;
    private final UserRepository userRepository;
    private final SkinTroubleElementsRepository skinTroubleElementsRepository;


    @Transactional
    public void currentSkinStatus(SkinNowStatusRequestDto requestDto, UserDetailsImpl userDetails) throws PlaluvsException {
        SkinType findSkinType = findDailySkinType(userDetails);

        updateSkinNowStatus(requestDto.getSkinId(), findSkinType);

        log.info("skinType = {}", findSkinType);
    }

    private void updateSkinNowStatus(Long id, SkinType findSkinType) throws PlaluvsException {
        CurrentSkinStatus currentSkinStatus = CurrentSkinStatus.findQuestionOne(id);
        CurrentSkinStatus nowSkinStatus = findSkinType.getCurrentSkinStatus();

        if( nowSkinStatus == null){
            updateCurrentStatusByCurrentSkinStatus(currentSkinStatus, findSkinType);
        }
        else{
            if(nowSkinStatus.getId() == 1L) {
                findSkinType.setOilIndicateScore( findSkinType.getOilIndicateScore() + 2);
                findSkinType.setDryScore( findSkinType.getDryScore() + 1);
            }
            else if  (nowSkinStatus.getId()== 2L){
                findSkinType.setOilIndicateScore( findSkinType.getOilIndicateScore() + 1);
                findSkinType.setDryScore( findSkinType.getDryScore() + 1);
            }
            else if (nowSkinStatus.getId() == 3L){
                findSkinType.setOilIndicateScore( findSkinType.getOilIndicateScore() + 2);
            }
            else if (nowSkinStatus.getId() == 4L){
                findSkinType.setOilIndicateScore( findSkinType.getOilIndicateScore() + 1);
                findSkinType.setDryScore( findSkinType.getDryScore() + 1);
            }
            updateCurrentStatusByCurrentSkinStatus(currentSkinStatus, findSkinType);
        }
    }

    private void updateCurrentStatusByCurrentSkinStatus(CurrentSkinStatus currentSkinStatus, SkinType findSkinType) {
        findSkinType.setCurrentSkinStatus(currentSkinStatus);

        if(currentSkinStatus.getId() == 1L) {
            findSkinType.setOilIndicateScore( findSkinType.getOilIndicateScore() - 2);
            findSkinType.setDryScore( findSkinType.getDryScore() - 1);
        }
        else if  (currentSkinStatus.getId()== 2L){
            findSkinType.setOilIndicateScore( findSkinType.getOilIndicateScore() - 1);
            findSkinType.setDryScore( findSkinType.getDryScore() - 1);
        }
        else if (currentSkinStatus.getId() == 3L){
            findSkinType.setOilIndicateScore( findSkinType.getOilIndicateScore() - 1);
        }
        else if (currentSkinStatus.getId() == 4L){
            findSkinType.setOilIndicateScore( findSkinType.getOilIndicateScore() - 1);
            findSkinType.setDryScore( findSkinType.getDryScore() - 1);
        }
    }


    public void skinWorryUpdate(SkinWorryRequestDto requestDto, UserDetailsImpl userDetails) throws PlaluvsException {
        SkinType findSkinType = findDailySkinType(userDetails);

        List<SkinTrouble> beforeSkinTroubleList = skinWorryRepository.findAllBySkinType(findSkinType);

        if(beforeSkinTroubleList == null){
            for (Long id : requestDto.getId()) {
                SkinTroubleEnum skinTroubleEnum = SkinTroubleEnum.findSkinTroubleEnum(id);

                SkinTrouble skinTrouble = new SkinTrouble(skinTroubleEnum, findSkinType);
                log.info("skinTrouble = {}", skinTrouble);
                saveSkinWorry(findSkinType, id, skinTrouble);
            }
        }
        else{
            for (SkinTrouble skinTrouble : beforeSkinTroubleList) {
                deleteSkinTroble(findSkinType, skinTrouble);
            }
            skinWorryRepository.deleteAll(beforeSkinTroubleList);

            for (Long id : requestDto.getId()) {
                SkinTroubleEnum skinTroubleEnum = SkinTroubleEnum.findSkinTroubleEnum(id);

                SkinTrouble skinTrouble = new SkinTrouble(skinTroubleEnum, findSkinType);
                log.info("skinTrouble = {}", skinTrouble);
                saveSkinWorry(findSkinType, id, skinTrouble);
            }
        }

    }

    private void deleteSkinTroble(SkinType findSkinType, SkinTrouble skinTrouble) {
        if(skinTrouble.getTrouble().getId() == 1L) {
            findSkinType.setOilIndicateScore(findSkinType.getOilIndicateScore() + 1);
            findSkinType.setSensitivityScore(findSkinType.getSensitivityScore() + 1);
        }
        else if (skinTrouble.getTrouble().getId() == 2L){
            findSkinType.setDryScore(findSkinType.getDryScore() + 1 );
            findSkinType.setWinkleScore(findSkinType.getWinkleScore() + 1);
        }
        else if (skinTrouble.getTrouble().getId() == 3L){
            findSkinType.setSensitivityScore(findSkinType.getSensitivityScore() + 1 );
        }
        else if (skinTrouble.getTrouble().getId() == 4L){
            findSkinType.setPigmentScore(findSkinType.getPigmentScore() + 1);
        }
        else if (skinTrouble.getTrouble().getId() == 5L){
            findSkinType.setOilIndicateScore(findSkinType.getOilIndicateScore() + 1 );
        }
    }

    @Transactional
    public void saveSkinWorry(SkinType findSkinType, Long id, SkinTrouble skinTrouble) {
        if(id == 1L) {
            findSkinType.setOilIndicateScore(findSkinType.getOilIndicateScore() - 1);
            findSkinType.setSensitivityScore(findSkinType.getSensitivityScore() -1);
            skinWorryRepository.save(skinTrouble);
            for (TroubleSkinElements value : TroubleSkinElements.values()) {
                Elements byKorean = elementsRepository.findByKorean(value.getName());
                if(byKorean == null)
                    continue;
                SkinTroubleElements skinTroubleElements = new SkinTroubleElements(skinTrouble, byKorean);
                skinTroubleElementsRepository.save(skinTroubleElements);
            }
        }
        else if (id == 2L){
            findSkinType.setDryScore(findSkinType.getDryScore() -1 );
            findSkinType.setWinkleScore(findSkinType.getWinkleScore() -1);
            skinWorryRepository.save(skinTrouble);
            for (WrinklesSkinElements value : WrinklesSkinElements.values()) {
                Elements byKorean = elementsRepository.findByKorean(value.getName());
                if(byKorean == null)
                    continue;
                SkinTroubleElements skinTroubleElements = new SkinTroubleElements(skinTrouble, byKorean);
                skinTroubleElementsRepository.save(skinTroubleElements);
            }
        }
        else if (id == 3L){
            findSkinType.setSensitivityScore(findSkinType.getSensitivityScore() -1 );
            skinWorryRepository.save(skinTrouble);
            for (SensitiveSkinElements value : SensitiveSkinElements.values()) {
                Elements byKorean = elementsRepository.findByKorean(value.getName());
                if(byKorean == null)
                    continue;
                SkinTroubleElements skinTroubleElements = new SkinTroubleElements(skinTrouble, byKorean);
                skinTroubleElementsRepository.save(skinTroubleElements);
            }
        }
        else if (id == 4L){
            findSkinType.setPigmentScore(findSkinType.getPigmentScore() -1);
            skinWorryRepository.save(skinTrouble);
            for (PigmentationSkinElements value : PigmentationSkinElements.values()) {
                Elements byKorean = elementsRepository.findByKorean(value.getName());
                if(byKorean == null)
                    continue;
                SkinTroubleElements skinTroubleElements = new SkinTroubleElements(skinTrouble, byKorean);
                skinTroubleElementsRepository.save(skinTroubleElements);
            }
        }
        else if (id == 5L){
            findSkinType.setOilIndicateScore(findSkinType.getOilIndicateScore() -1 );
            skinWorryRepository.save(skinTrouble);
            for (UnbalanceSkinElements value : UnbalanceSkinElements.values()) {
                Elements byKorean = elementsRepository.findByKorean(value.getName());
                if(byKorean == null)
                    continue;
                SkinTroubleElements skinTroubleElements = new SkinTroubleElements(skinTrouble, byKorean);
                skinTroubleElementsRepository.save(skinTroubleElements);
            }
        }


    }


    @Transactional
    public void skinDailyStatus(SkinDailyStatusRequestDto requestDto, UserDetailsImpl userDetails) throws PlaluvsException {
        SkinType findSkinType = findDailySkinType(userDetails);

        List<SkinDailyStatus> skinDailyStatusList = skinDailyStatusRepository.findAllBySkinType(findSkinType);

        if(skinDailyStatusList == null){
            for (Long id : requestDto.getId()) {
                SkinDailyStatusEnum dailySkinEnum = SkinDailyStatusEnum.findDailySkinEnum(id);

                SkinDailyStatus skinDailyStatus = new SkinDailyStatus(dailySkinEnum, findSkinType);
                log.info("skinTrouble = {}", skinDailyStatus);
                saveDailyStatus(findSkinType, id, skinDailyStatus);
            }
        }
        else{
            for ( SkinDailyStatus skinDailyStatus: skinDailyStatusList) {
                deleteSkinDailyStatus(findSkinType, skinDailyStatus);
            }
            skinDailyStatusRepository.deleteAll(skinDailyStatusList);

            for (Long id : requestDto.getId()) {
                SkinDailyStatusEnum dailySkinEnum = SkinDailyStatusEnum.findDailySkinEnum(id);

                SkinDailyStatus skinDailyStatus = new SkinDailyStatus(dailySkinEnum, findSkinType);
                log.info("skinTrouble = {}", skinDailyStatus);
                saveDailyStatus(findSkinType, id, skinDailyStatus);
            }
        }
    }

    private void deleteSkinDailyStatus(SkinType findSkinType, SkinDailyStatus skinDailyStatus) {
        if(skinDailyStatus.getSkinDaily().getId() == 1L) {
            findSkinType.setSensitivityScore(findSkinType.getSensitivityScore() + 1);
        }
        else if (skinDailyStatus.getSkinDaily().getId() == 2L){
            findSkinType.setDryScore(findSkinType.getDryScore() + 1 );
        }
        else if (skinDailyStatus.getSkinDaily().getId() == 3L){
            findSkinType.setOilIndicateScore( findSkinType.getOilIndicateScore() + 2);
        }
        else if (skinDailyStatus.getSkinDaily().getId() == 4L){
            findSkinType.setOilIndicateScore( findSkinType.getOilIndicateScore() + 1);
        }
    }

    private void saveDailyStatus(SkinType findSkinType, Long id, SkinDailyStatus skinDailyStatus) {
        if(id == 1L) {
            findSkinType.setSensitivityScore(findSkinType.getSensitivityScore() -1);
        }
        else if (id == 2L){
            findSkinType.setDryScore(findSkinType.getDryScore() -1 );
        }
        else if (id == 3L){
            findSkinType.setOilIndicateScore( findSkinType.getOilIndicateScore() - 2);
        }
        else if (id == 4L){
            findSkinType.setOilIndicateScore( findSkinType.getOilIndicateScore() -1);
        }

        skinDailyStatusRepository.save(skinDailyStatus);
    }


    public SkinStatusResponseDto skinStatus(UserDetailsImpl userDetails) throws PlaluvsException {
        UserDetailsImpl.UserCheck(userDetails);

        SkinType dailySkinTYpe = skinTypeRepository.findDailySkinTypeException(userDetails);
        List<SkinElementsDto> elementsDtoList = elementsRepository.findSkinElementsDtoListBySkinTypeGoodElements(dailySkinTYpe);
        for (SkinElementsDto skinElementsDto : elementsDtoList) {
            skinElementsDto.setImg(getImgSRc(skinElementsDto.getLevel()));
        }

        
        SkinStatusResponseDto result = new SkinStatusResponseDto(
                dailySkinTYpe.getScore().toString() + "점",
                "수분이 부족한 중/복합성 피부네요. 피부에 수분이 부족한 탓에 색소침착이 있네요. 다행히 피부가 저항성을 갖고 있어 외부 환경에 아주 민감 하진 않아요." +
                        "그래도 주름과 색소침착을 예방하기 위해 자외선은 각별히 신경을 써주셔야해요.외출 시에는 꼭! 자외선 차단제를 자주 덧 발라주세요",
                dailySkinTYpe.getOilIndicateScore() * 100 /9,
                dailySkinTYpe.getDryScore()  * 100 / 5,
                dailySkinTYpe.getSensitivityScore() * 100 / 9,
                dailySkinTYpe.getPigmentScore() * 100 / 2,
                dailySkinTYpe.getWinkleScore() *100 /3,
                elementsDtoList
        );

        return result;
    }
    private String getImgSRc(String level) {
        String low = "https://plaluvs-image.s3.ap-northeast-2.amazonaws.com/rank/row_rank.png";
        String middle = "https://plaluvs-image.s3.ap-northeast-2.amazonaws.com/rank/middle_rank.png";
        String high = "https://plaluvs-image.s3.ap-northeast-2.amazonaws.com/rank/high_rank.png";

        if( level.contains("1")){
            return low;
        }
        else if( level.contains("2")){
            return low;
        }

        else if( level.contains("3")){
            return middle;
        }

        else if( level.contains("4")){
            return middle;
        }

        else if( level.contains("5")){
            return middle;
        }

        else if( level.contains("6")){
            return middle;
        }

        else if( level.contains("7")){
            return high;
        }

        else if( level.contains("8")){
            return high;
        }

        else if( level.contains("9")){
            return high;
        }

        else if( level.contains("10")){
            return high;
        }

        else if( level.contains("-")){
            return low;
        }

        return "";
    }

    public SkinStatusListResponseDto skinStatusList(UserDetailsImpl userDetails) {
        Sort createdAt = Sort.by(Sort.Direction.DESC, "createdAt");

        List<SkinType> skinTypeList = skinTypeRepository.findAllByUserOrderByCreatedAt(userDetails.getUser(), createdAt);
        List<StatusList> statusList = new ArrayList<>();

        for (SkinType skinType : skinTypeList) {
            StatusList newSatusList = new StatusList(Timestamped.TimeToString(skinType.getCreatedAt(),
                    "MM월 dd일"), skinType.getScore().toString() + "점");
            statusList.add(newSatusList);
        }

        Long minusScore = 0L;
        if(skinTypeList.size() >= 2){
            SkinType latest = skinTypeList.get(skinTypeList.size() - 1);
            SkinType a = skinTypeList.get(skinTypeList.size() - 2);
            minusScore = latest.getScore() - a.getScore();
        }
        String             str1 = "개선사항이 없습니다.";
        String str2 = "개선사항이 없습니다.";
        if(minusScore == 0){
            str1 = "개선사항이 없습니다.";
            str2 = "개선사항이 없습니다.";
        }
        else if (minusScore > 0 ){
            str1= "지난 주 보다 점수가" + minusScore +"점 만큼 상승했어요.";
            str2 = "어제보다 민감한 피부가 개선되었어요";
        }
        else if (minusScore < 0 ){
            str1= "지난 주 보다 점수가" + -minusScore +"점 만큼 떨어졌어요.";
            str2 = "어제보다 피부가 안좋아졌어요";
        }

        SkinStatusListResponseDto result = new SkinStatusListResponseDto(statusList, str1, str2);


        return result;
    }


    private SkinType findDailySkinType(UserDetailsImpl userDetails) {
        LocalDateTime startDatetime = LocalDateTime.of(LocalDate.now(), LocalTime.of(0,0,0)); //오늘 00:00:00
        LocalDateTime endDatetime = LocalDateTime.of(LocalDate.now(), LocalTime.of(23,59,59)); //오늘 23:59:59

        SkinType findDailySkinType = skinTypeRepository.findTopByUserAndCreatedAtBetween(userDetails.getUser(), startDatetime, endDatetime);

        SkinType firstSkinType = skinTypeRepository.findTopByUserOrderByCreatedAtAsc(userDetails.getUser());
        CurrentSkinStatus currentSkinStatus = null;

        if(firstSkinType != null)
            currentSkinStatus = firstSkinType.getCurrentSkinStatus();

        if(findDailySkinType == null){
            SkinType newSkinType = new SkinType(currentSkinStatus, userDetails.getUser());
            skinTypeRepository.save(newSkinType);
            return newSkinType;
        }
        else
            return findDailySkinType;

    }

    @Transactional
    public void skinDailyStimulation(SkinDailyStimulationRequestDto requestDto, UserDetailsImpl userDetails) throws PlaluvsException {
        SkinType findSkinType = findDailySkinType(userDetails);
        List<SkinDailyStimulation> skinDailyStimulationList = skinDailyStimulationRepository.findAllBySkinType(findSkinType);

        if(skinDailyStimulationList == null){
            for (Long id : requestDto.getId()) {
                SkinDailyStimulationEnum skinDailyStimulationEnum = SkinDailyStimulationEnum.findSkinDailyStimulationEnum(id);

                SkinDailyStimulation skinDailyStimulation = new SkinDailyStimulation(skinDailyStimulationEnum, findSkinType);
                log.info("skinTrouble = {}", skinDailyStimulation);
                saveDailyStimulation(findSkinType, id, skinDailyStimulation);
            }
        }
        else{
            for ( SkinDailyStimulation skinDailyStimulation: skinDailyStimulationList) {
                deleteSkinDailyStimulation(findSkinType, skinDailyStimulation);
            }
            skinDailyStimulationRepository.deleteAll(skinDailyStimulationList);

            for (Long id : requestDto.getId()) {
                SkinDailyStimulationEnum skinDailyStimulationEnum = SkinDailyStimulationEnum.findSkinDailyStimulationEnum(id);

                SkinDailyStimulation skinDailyStimulation = new SkinDailyStimulation(skinDailyStimulationEnum, findSkinType);
                log.info("skinTrouble = {}", skinDailyStimulation);
                saveDailyStimulation(findSkinType, id, skinDailyStimulation);
            }
        }
    }

    private void deleteSkinDailyStimulation(SkinType findSkinType, SkinDailyStimulation skinDailyStimulation) {
        if(skinDailyStimulation.getSkinDaily().getId() == 1L) {
            findSkinType.setSensitivityScore(findSkinType.getSensitivityScore() + 1);
        }
        else if (skinDailyStimulation.getSkinDaily().getId() == 2L){
            findSkinType.setSensitivityScore(findSkinType.getSensitivityScore() + 1);
        }
        else if (skinDailyStimulation.getSkinDaily().getId() == 3L){
            findSkinType.setOilIndicateScore( findSkinType.getOilIndicateScore() - 1);
            findSkinType.setSensitivityScore(findSkinType.getSensitivityScore() + 1);
        }
        else if (skinDailyStimulation.getSkinDaily().getId() == 4L){
            findSkinType.setSensitivityScore(findSkinType.getSensitivityScore() + 1);
        }
    }

    private void saveDailyStimulation(SkinType findSkinType, Long id, SkinDailyStimulation skinDailyStimulation) {
        if(id == 1L) {
            findSkinType.setSensitivityScore(findSkinType.getSensitivityScore() -1);
        }
        else if (id == 2L){
            findSkinType.setSensitivityScore(findSkinType.getSensitivityScore() -1);
        }
        else if (id == 3L){
            findSkinType.setOilIndicateScore( findSkinType.getOilIndicateScore() + 1);
            findSkinType.setSensitivityScore(findSkinType.getSensitivityScore() -1);
        }
        else if (id == 4L){
            findSkinType.setSensitivityScore(findSkinType.getSensitivityScore() -1);
        }

        skinDailyStimulationRepository.save(skinDailyStimulation);
    }

    @Transactional
    public void skinSelfCheck(SkinDailySefCheckRequestDto requestDto, UserDetailsImpl userDetails) throws PlaluvsException {
        SkinType findSkinType = findDailySkinType(userDetails);

        findSkinType.setSelfScore(requestDto.getScore());
    }


    @Transactional
    public String skinBoumanCalucluate(UserDetailsImpl userDetails) throws PlaluvsException {
        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow(
                () -> new PlaluvsException(ErrorCode.USER_NOT_FOUND)
        );

        SkinType dailySkinType = findDailySkinType(userDetails);

        String key = getBoumanKey(dailySkinType);
        log.info("key = {}", key);
        dailySkinType.setBouman(Bouman.findBoumanBySkinType(key));


        Double selfScore1 = dailySkinType.getSelfScore();
        if(selfScore1 == null)
            throw new PlaluvsException(ErrorCode.SKIN_TYPE_NOT_FOUND);

        double selfScore = dailySkinType.getSelfScore() * 20;
        dailySkinType.setScore(dailySkinType.getBouman().getScore()*40/100 + (long)selfScore*20/100 + 40L);

        return dailySkinType.getBouman().getName();
    }

    private String getBoumanKey(SkinType dailySkinType) {
        String key = "";
        if(dailySkinType.getOilIndicateScore() <= dailySkinType.getDryScore()){
            key += "O";
        }
        else
            key += "D";

        if(dailySkinType.getSensitivityScore() < 0){
            key += "S";
        }
        else
            key += "R";

        if(dailySkinType.getPigmentScore() < 0){
            key += "P";
        }
        else
            key += "N";
        if(dailySkinType.getWinkleScore() < 0){
            key += "W";
        }
        else {
            key += "T";
        }
        return key;
    }
}
