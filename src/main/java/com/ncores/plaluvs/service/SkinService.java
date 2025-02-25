package com.ncores.plaluvs.service;

import com.ncores.plaluvs.controller.skin.dto.*;
import com.ncores.plaluvs.domain.*;
import com.ncores.plaluvs.domain.dto.SkinNowStatusRequestDto;
import com.ncores.plaluvs.domain.dto.SkinWorryRequestDto;
import com.ncores.plaluvs.domain.skintype.Bouman;
import com.ncores.plaluvs.domain.skintype.CurrentSkinStatus;
import com.ncores.plaluvs.domain.skintype.SkinTypeEnum;
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
import com.ncores.plaluvs.repository.skinType.SkinTypeRepository;
import com.ncores.plaluvs.security.UserDetailsImpl;
import com.ncores.plaluvs.service.skin.text.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

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
    private final PhotoRepository photoRepository;


    @Transactional
    public void currentSkinStatus(SkinNowStatusRequestDto requestDto, User user, LocalDateTime createdAt) throws PlaluvsException {
        SkinType findSkinType = findDailySkinType(user, createdAt);

        updateSkinNowStatus(requestDto.getSkinId(), findSkinType);

        log.info("skinType = {}", findSkinType);
    }

    private void updateSkinNowStatus(Long id, SkinType findSkinType) throws PlaluvsException {
        CurrentSkinStatus currentSkinStatus = CurrentSkinStatus.findQuestionOne(id);
        CurrentSkinStatus nowSkinStatus = findSkinType.getCurrentSkinStatus();

        if (nowSkinStatus != null) {
            rollbackCurrentStatus(findSkinType, nowSkinStatus);
        }
        updateCurrentStatusByCurrentSkinStatus(currentSkinStatus, findSkinType);
    }

    private void rollbackCurrentStatus(SkinType findSkinType, CurrentSkinStatus nowSkinStatus) {
        if (nowSkinStatus.getId() == 1L) {
            findSkinType.setOilIndicateScore(findSkinType.getOilIndicateScore() + 2);
            findSkinType.setDryScore(findSkinType.getDryScore() + 1);
        } else if (nowSkinStatus.getId() == 2L) {
            findSkinType.setOilIndicateScore(findSkinType.getOilIndicateScore() + 1);
            findSkinType.setDryScore(findSkinType.getDryScore() + 1);
        } else if (nowSkinStatus.getId() == 3L) {
            findSkinType.setOilIndicateScore(findSkinType.getOilIndicateScore() + 2);
        } else if (nowSkinStatus.getId() == 4L) {
            findSkinType.setOilIndicateScore(findSkinType.getOilIndicateScore() + 1);
            findSkinType.setDryScore(findSkinType.getDryScore() + 1);
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
            findSkinType.setOilIndicateScore( findSkinType.getOilIndicateScore() - 2);
        }
        else if (currentSkinStatus.getId() == 4L){
            findSkinType.setOilIndicateScore( findSkinType.getOilIndicateScore() - 1);
            findSkinType.setDryScore( findSkinType.getDryScore() - 1);
        }
    }


    public void skinWorryUpdate(SkinWorryRequestDto requestDto, User user, LocalDateTime createdAt) throws PlaluvsException {
        log.info("id = {}", requestDto.getId());
        SkinType findSkinType = findDailySkinType(user, createdAt);

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
    public void skinDailyStatus(SkinDailyStatusRequestDto requestDto, User user, LocalDateTime createdAt) throws PlaluvsException {
        SkinType findSkinType = findDailySkinType(user, createdAt);

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


    @Transactional
    public SkinStatusResponseDto skinStatus(UserDetailsImpl userDetails) throws PlaluvsException {
        UserDetailsImpl.UserCheck(userDetails);

        SkinType dailySkinTYpe = skinTypeRepository.findDailySkinTypeException(userDetails.getUser());
        if(dailySkinTYpe.getTotalScore() == null){
            throw new PlaluvsException(ErrorCode.SKIN_TYPE_NOT_FOUND);
        }

        List<SkinElementsDto> elementsDtoList = elementsRepository.findSkinElementsDtoListBySkinTypeGoodElements(dailySkinTYpe, userDetails);

        for (SkinElementsDto skinElementsDto : elementsDtoList) {
            skinElementsDto.setImg(getImgSRc(skinElementsDto.getLevel()));
        }

        long oilScore = (dailySkinTYpe.getOilIndicateScore() * 100) / 11;
        long dryScore = (dailySkinTYpe.getDryScore()  * 100) / 5;
        long senScore = (dailySkinTYpe.getSensitivityScore() * 100) / 7;
        long pigScore = (dailySkinTYpe.getPigmentScore() * 100) / 2;
        long winScore = (dailySkinTYpe.getWinkleScore() *100) /1;

        String text ="";

        Long age = userDetails.getUser().getAge();
        int year = LocalDateTime.now().getYear();
        long a = year - age;


        text = getString(dailySkinTYpe, oilScore, dryScore, senScore, pigScore, winScore, text, a);


        SkinStatusResponseDto result = new SkinStatusResponseDto(
                dailySkinTYpe.getTotalScore().toString() + "점",
                text,
                oilScore,
                dryScore,
                senScore,
                pigScore,
                winScore,
                elementsDtoList
        );

        return result;
    }

    private String getString(SkinType dailySkinTYpe, long oilScore, long dryScore, long senScore, long pigScore, long winScore, String text, long a) {

        if(dailySkinTYpe.getOilDryText() == null){
            //age text
            if(a >= 25 && a <30){
                dailySkinTYpe.setAgeText(Long.valueOf(ageMinText.getEnum().getId()));
            }
            else if(a >=30 && a <40){
                dailySkinTYpe.setAgeText(Long.valueOf(ageMiddleText.getEnum().getId()));
            }
            else{
                dailySkinTYpe.setAgeText(Long.valueOf(ageMaxText.getEnum().getId()));
            }

            //oil,dry text
            if(oilScore > dryScore){
                dailySkinTYpe.setOilDryText( Long.valueOf(oilText.getEnum().getId()));
            }
            else{
                dailySkinTYpe.setOilDryText( Long.valueOf(dryText.getEnum().getId()));
            }

            //sen text
            if(senScore < 50){
                dailySkinTYpe.setSenText( Long.valueOf(senMinText.getEnum().getId()));
            }
            else{
                dailySkinTYpe.setSenText( Long.valueOf(senMaxText.getEnum().getId()));
            }

            //pig text
            if(pigScore > 50){
                dailySkinTYpe.setPigText( Long.valueOf(pigMaxText.getEnum().getId()));
            }
            //win text
            if(winScore > 50){
                dailySkinTYpe.setWinText( Long.valueOf(winMaxText.getEnum().getId()));
            }
            else{
                dailySkinTYpe.setWinText( Long.valueOf(winMinText.getEnum().getId()));
            }

            //trouble text
            long troubleScore = dailySkinTYpe.getTotalScore() - ((long) dailySkinTYpe.getScore() * 6 / 10);
            if(troubleScore < 20){
                dailySkinTYpe.setTroubleText( Long.valueOf(troubleMaxText.getEnum().getId()));
            }
            else if (troubleScore > 20 && troubleScore <=30){
                dailySkinTYpe.setTroubleText( Long.valueOf(troubleMiddleText.getEnum().getId()));
            }
            else{
                dailySkinTYpe.setTroubleText( Long.valueOf(troubleMinText.getEnum().getId()));
            }
        }

        //age text
        if(a >= 25 && a <30){
            text += ageMinText.getEnum(dailySkinTYpe.getAgeText()).getText();
            text += " ";
        }
        else if(a >=30 && a <40){
            text += ageMiddleText.getEnum(dailySkinTYpe.getAgeText()).getText();
            text += " ";
        }
        else{
            text += ageMaxText.getEnum(dailySkinTYpe.getAgeText()).getText();
            text += " ";
        }

        //oil,dry text
        if(oilScore > dryScore){
            text += oilText.getEnum(dailySkinTYpe.getOilDryText()).getText();
            text += " ";
        }
        else{
            text += dryText.getEnum(dailySkinTYpe.getOilDryText()).getText();
            text += " ";
        }

        //sen text
        if(senScore < 50){
            text += senMinText.getEnum(dailySkinTYpe.getSenText()).getText();
            text += " ";
        }
        else{
            text += senMaxText.getEnum(dailySkinTYpe.getSenText()).getText();
            text += " ";
        }


        //pig text
        if(pigScore > 50){
            text += pigMaxText.getEnum(dailySkinTYpe.getPigText()).getText();
            text += " ";
        }

        //win text
        if(winScore > 50){
            text += winMaxText.getEnum(dailySkinTYpe.getWinText()).getText();
            text += " ";
        }
        else{
            text += winMinText.getEnum(dailySkinTYpe.getWinText()).getText();
        }

        //trouble text
        long troubleScore = dailySkinTYpe.getTotalScore() - ((long) dailySkinTYpe.getScore() * 6 / 10);
        if(troubleScore < 20){
            text += troubleMaxText.getEnum(dailySkinTYpe.getTroubleText()).getText();
            text += " ";
        }
        else if (troubleScore > 20 && troubleScore <=30){
            text += troubleMiddleText.getEnum(dailySkinTYpe.getTroubleText()).getText();
            text += " ";
        }
        else{
            text += troubleMinText.One.getText();
            text += " ";
        }

        return text;
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

    public PagingAveragingScoreResponseDto skinStatusList(UserDetailsImpl userDetails, Long page, String sort) throws PlaluvsException {
        PageRequest pageRequest = PageRequest.of(page.intValue(), 7);
        Page<SkinType> result =  skinTypeRepository.findAllCustom(userDetails, pageRequest, sort);
        List<Status> statusList = new ArrayList<>();

        SkinType todays = skinTypeRepository.findDailySkinTypeOrReturnNull(userDetails);


        String buttonColor = (todays != null && todays.getTotalScore() != null) ? "#F5EBE8": "#323632";
        String text = "hello world";

        // 데이터 없음 0, 오늘 안함 1, 있고 함 2
        Long statusData = 0L;

        Long maxIndex = 0L;
        Long max = 0L;
        Long minIndex = 0L;
        Long min = 100L;

        Long doNotTest = 0L;
        int i = 0;

        for (; i < result.getContent().size(); i++) {
            SkinType skinType = result.getContent().get(i);
            if(skinType.getTotalScore() != null){
                Status status = new Status(skinType.getTotalScore(), getDate(skinType.getCreatedAt()), "common");
                statusList.add(status);
            }

            else{
                doNotTest++;
                continue;
            }
        }
        for (int j = statusList.size(); j < 7; j++) {
            Status status = new Status(0L, j+3 + "달 전", "common");
            statusList.add(0,status);
        }

        Long k = 0L;
        Long trueSize =0L;
        for (Status status : statusList) {
            if(!status.getScore().equals(0L)){

                if(max < status.getScore()){
                    max = status.getScore();
                    maxIndex = k;
                }
                if(min > status.getScore()){
                    min = status.getScore();
                    minIndex = k;
                }
                trueSize++;
            }
            k++;
        }

        //오늘 함
        if(todays != null && todays.getTotalScore() != null){
            statusData = 2L;
            text = "첫 진단이에요!\n" +
                    "내일도 변화를 기록해 보세요";

            if(trueSize >=2){
                if(trueSize == 2 &&
                        statusList.get(statusList.size()-1).getScore().equals(statusList.get(statusList.size()-2).getScore())){
                    text = "두번째 진단도 성공!\n" +
                            "내일도 변화를 기록해 보세요";
                }
                else if (!maxIndex.equals(minIndex) && trueSize >=2 ){
                    Status today = statusList.get(statusList.size()-1);

                    if(today.getScore() > statusList.get(minIndex.intValue()).getScore()){
                        text = today.getDate() +" 피부 점수가\n" +
                                statusList.get(minIndex.intValue()).getDate()+ "보다" +
                                (today.getScore() - statusList.get(minIndex.intValue()).getScore())+"이 올랐어요";
                    }
                    else{
                        text = "꾸준히 관리하는 피부는\n" +
                                "시간이 지날수록 아름다워요";
                    }

                }
                else if(maxIndex.equals(minIndex) && trueSize >= 3){
                    text = "꾸준히 관리하는 피부는\n" +
                            "시간이 지날수록 아름다워요";
                }
            }
        }
        // 오늘 안함
        else{
            statusData = 1L;
            text = "꾸준한 피부 기록은\n" +
                    "피부 변화의 시작이에요";
            if(trueSize == 2){
                Status newSt = statusList.get(statusList.size()-1);
                Status oldSt = statusList.get(statusList.size()-2);

                if(!newSt.getScore().equals(oldSt.getScore())){
                    text = "피부 점수가 "+   (newSt.getScore() > oldSt.getScore() ? oldSt.getDate() : newSt.getDate()) +"보다\n"
                            + (newSt.getScore() < oldSt.getScore() ? oldSt.getDate() : newSt.getDate()) +"이 높았어요";
                }
            }
            else if( trueSize >= 3L){
                text = "최근 피부 점수가 "+ statusList.get(minIndex.intValue()).getDate()+"에 가장 낮았고\n" +
                        statusList.get(maxIndex.intValue()).getDate()+"에 가장 높았어요";
            }
        }

        if(trueSize != 0L){
            statusList.get(maxIndex.intValue()).setType("bold");
            statusList.get(minIndex.intValue()).setType("bold");
        }

        if(trueSize == 0L){
            statusData = 0L;
        }

        Collections.reverse(statusList);

        return new PagingAveragingScoreResponseDto(statusList.size(), text, buttonColor, statusData, result.getNumber(), result.getTotalPages(), statusList);
    }

    private String getDate(LocalDateTime createdAt){
        LocalDate createdAtLocalDate = createdAt.toLocalDate();
        LocalDate now = LocalDateTime.now().toLocalDate();

        long betWeenYear = ChronoUnit.YEARS.between(createdAtLocalDate, now);
        if(betWeenYear != 0){
            return "1년 전";
        }

        long betWeenMonth = ChronoUnit.MONTHS.between(createdAtLocalDate, now);
        if(betWeenMonth != 0 ){
            return betWeenMonth +"달 전";
        }

        long betWeenDay = ChronoUnit.DAYS.between(createdAtLocalDate, now);
        if(betWeenDay != 0){
            return betWeenDay +"일 전";
        }

        return "오늘";
    }


    private SkinType findDailySkinType(User user, LocalDateTime createdAt) {
        LocalDateTime startDatetime = null;
        LocalDateTime endDatetime = null;

        if(createdAt == null) {
            startDatetime = LocalDateTime.of(LocalDate.now(), LocalTime.of(0, 0, 0));; //오늘 00:00:00
            endDatetime = LocalDateTime.of(LocalDate.now(), LocalTime.of(23, 59, 59)); //오늘 23:59:59
        }
        else {
            startDatetime = LocalDateTime.of(createdAt.toLocalDate(), LocalTime.of(0, 0, 0));
            endDatetime = LocalDateTime.of(createdAt.toLocalDate(), LocalTime.of(23, 59, 59));
        }

        SkinType findDailySkinType = skinTypeRepository.findTopByUserAndCreatedAtBetween(user, startDatetime, endDatetime);

        SkinType firstSkinType = skinTypeRepository.findTopByUserOrderByCreatedAtAsc(user);
        CurrentSkinStatus currentSkinStatus = null;

        if(firstSkinType != null)
            currentSkinStatus = firstSkinType.getCurrentSkinStatus();

        if(findDailySkinType == null){
            SkinType newSkinType = new SkinType(currentSkinStatus, user, createdAt);
            skinTypeRepository.save(newSkinType);
            return newSkinType;
        }
        else
            return findDailySkinType;
    }

    private void setLocalDateTime( LocalDateTime startDatetime, LocalDateTime endDatetime, LocalDateTime createdAt){
        if(createdAt == null) {
            startDatetime = LocalDateTime.of(LocalDate.now(), LocalTime.of(0, 0, 0));; //오늘 00:00:00
            endDatetime = LocalDateTime.of(LocalDate.now(), LocalTime.of(23, 59, 59)); //오늘 23:59:59
        }
        else {
            startDatetime = LocalDateTime.of(createdAt.toLocalDate(), LocalTime.of(0, 0, 0));
            endDatetime = LocalDateTime.of(createdAt.toLocalDate(), LocalTime.of(23, 59, 59));
        }
    }




    @Transactional
    public void skinDailyStimulation(SkinDailyStimulationRequestDto requestDto, User user, LocalDateTime createdAt) throws PlaluvsException {
        SkinType findSkinType = findDailySkinType(user, createdAt);
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
            findSkinType.setSensitivityScore(findSkinType.getSensitivityScore() + 1);
        }
        else if (skinDailyStimulation.getSkinDaily().getId() == 4L){
            findSkinType.setSensitivityScore(findSkinType.getSensitivityScore() + 1);
        }
        else if (skinDailyStimulation.getSkinDaily().getId() == 5L){
            findSkinType.setPigmentScore(findSkinType.getPigmentScore() + 1);
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
            findSkinType.setSensitivityScore(findSkinType.getSensitivityScore() -1);
        }
        else if (id == 4L){
            findSkinType.setSensitivityScore(findSkinType.getSensitivityScore() -1);
        }
        else if (skinDailyStimulation.getSkinDaily().getId() == 5L){
            findSkinType.setPigmentScore(findSkinType.getPigmentScore() - 1);
        }

        skinDailyStimulationRepository.save(skinDailyStimulation);
    }

    @Transactional
    public void skinSelfCheck(SkinDailySefCheckRequestDto requestDto, User user, LocalDateTime createdAt) throws PlaluvsException {
        SkinType findSkinType = findDailySkinType(user, createdAt);

        findSkinType.setSelfScore(requestDto.getScore());
    }


    @Transactional
    public String skinBoumanCalucluate(User user, LocalDateTime createdAt) throws PlaluvsException {
        User findID = userRepository.findById(user.getId()).orElseThrow(
                () -> new PlaluvsException(ErrorCode.USER_NOT_FOUND)
        );

        SkinType dailySkinType = findDailySkinType(findID, createdAt);

        String key = getBoumanKey(dailySkinType);
        log.info("key = {}", key);
        dailySkinType.setBouman(Bouman.findBoumanBySkinType(key));


        Double selfScore1 = dailySkinType.getSelfScore() * 20;
        if(selfScore1 == null)
            throw new PlaluvsException(ErrorCode.SKIN_TYPE_NOT_FOUND);

        long oil = (dailySkinType.getOilIndicateScore() * 100) / 11;
        long dry = (dailySkinType.getDryScore() * 100) / 5;
        long sen = (dailySkinType.getSensitivityScore() * 100) / 7;
        long pig = (dailySkinType.getPigmentScore() * 100) / 2;
        long win = (dailySkinType.getWinkleScore() * 100) / 1;

        long score = (oil + dry + sen + pig + win) / 5;

        dailySkinType.setScore(score * 80/100 + (long)selfScore1.intValue()*20/100);

        return dailySkinType.getBouman().getName();
    }

    private String getBoumanKey(SkinType dailySkinType) {
        String key = "";
        if(dailySkinType.getOilIndicateScore() <= dailySkinType.getDryScore()){
            key += "O";
        }
        else
            key += "D";

        if(dailySkinType.getSensitivityScore() < 4){
            key += "S";
        }
        else
            key += "R";

        if(dailySkinType.getPigmentScore() < 1){
            key += "P";
        }
        else
            key += "N";
        if(dailySkinType.getWinkleScore() < 1){
            key += "W";
        }
        else {
            key += "T";
        }
        return key;
    }

    public skinStatusBoumanResponseDto skinStatusBouman(UserDetailsImpl userDetails) throws PlaluvsException {
        LocalDateTime startDatetime;
        LocalDateTime endDatetime;

        LocalDateTime startDatetimeWeek = LocalDateTime.of(LocalDate.now().minusWeeks(1), LocalTime.of(0,0,0));
        LocalDateTime endDatetimeWeek = LocalDateTime.of(LocalDate.now(), LocalTime.of(23,59,59));

        LocalDateTime startDatetimeWeekAgo = LocalDateTime.of(LocalDate.now().minusWeeks(2), LocalTime.of(0,0,0)); //오늘 00:00:00
        LocalDateTime endDatetimeWeekAgo = LocalDateTime.of(LocalDate.now().minusWeeks(1), LocalTime.of(23,59,59)); //오늘 23:59:59

        LocalDateTime endDatetimeMonth = LocalDateTime.of(LocalDate.now().minusMonths(1), LocalTime.of(23,59,59)); //오늘 23:59:59

        startDatetime = LocalDateTime.of(LocalDate.now().minusWeeks(1L), LocalTime.of(0,0,0)); //오늘 00:00:00
        endDatetime = LocalDateTime.of(LocalDate.now(), LocalTime.of(23,59,59)); //오늘 23:59:59

        List<ScoreData> dryResult = skinTypeRepository.findSkinStatusBoumanCustom(userDetails, startDatetime, endDatetime, SkinTypeEnum.DRY);
        Setting(startDatetimeWeek, endDatetimeWeek,
                startDatetimeWeekAgo, endDatetimeWeekAgo,
                 endDatetimeMonth,
                dryResult, 100L, 5L );

        List<ScoreData> oilResult = skinTypeRepository.findSkinStatusBoumanCustom(userDetails, startDatetime, endDatetime, SkinTypeEnum.OIL);
        Setting(startDatetimeWeek, endDatetimeWeek,
                startDatetimeWeekAgo, endDatetimeWeekAgo,
                 endDatetimeMonth,
                oilResult, 100L, 11L );


        List<ScoreData> senResult = skinTypeRepository.findSkinStatusBoumanCustom(userDetails, startDatetime, endDatetime, SkinTypeEnum.SEN);
        Setting(startDatetimeWeek, endDatetimeWeek,
                startDatetimeWeekAgo, endDatetimeWeekAgo,
                 endDatetimeMonth,
                senResult, 100L, 7L );


        List<ScoreData> winResult = skinTypeRepository.findSkinStatusBoumanCustom(userDetails, startDatetime, endDatetime, SkinTypeEnum.WIN);
        Setting(startDatetimeWeek, endDatetimeWeek,
                startDatetimeWeekAgo, endDatetimeWeekAgo,
                 endDatetimeMonth,
                winResult, 100L, 1L );


        List<ScoreData> pigResult = skinTypeRepository.findSkinStatusBoumanCustom(userDetails, startDatetime, endDatetime, SkinTypeEnum.PIG);
        Setting(startDatetimeWeek, endDatetimeWeek,
                startDatetimeWeekAgo, endDatetimeWeekAgo,
                 endDatetimeMonth,
                pigResult, 100L, 2L );


        skinStatusBoumanResponseDto skinStatusBoumanResponseDto = new skinStatusBoumanResponseDto(dryResult, oilResult, senResult, pigResult, winResult);

        return skinStatusBoumanResponseDto;
    }


    private void Setting(LocalDateTime startDatetimeWeek, LocalDateTime endDatetimeWeek,
                         LocalDateTime startDatetimeWeekAgo, LocalDateTime endDatetimeWeekAgo,
                         LocalDateTime endDatetimeMonth,
                         List<ScoreData> dryResult, Long rate1, Long rate2) {

        Long beforeScore =null;

        Collections.sort(dryResult, new  ScoreDataComparator());

        for (ScoreData scoreData : dryResult) {

            Long dryScore = scoreData.getScore() * rate1 /rate2 ;

            if(dryScore.equals(99L)){
                dryScore = 100L;
            }

            if(dryScore.equals(99L)){
                dryScore = 100L;
            }
            scoreData.setScore(dryScore);
            LocalDateTime createdAt = scoreData.getCreatedAt();

            //이번 주 일이라면
            if(createdAt.isAfter(startDatetimeWeek) && createdAt.isBefore(endDatetimeWeek)){
                scoreData.setTag("이번 주 엔 "+dryScore+"점이에요");
                scoreData.setColor("323632");
            }
            //일주일전엔
            else if(createdAt.isAfter(startDatetimeWeekAgo)&& createdAt.isBefore(endDatetimeWeekAgo)){
                scoreData.setTag("일주일 전 엔 "+ dryScore+ "점이에요");
                scoreData.setColor("607060");
            }
            //한달전엔
            else if(createdAt.isBefore(endDatetimeMonth)){
                scoreData.setTag("한달 전 엔 "+ dryScore+ "점이었어요");
                scoreData.setColor("8D998D");
            }

            if(beforeScore != null){
                scoreData.setRate(dryScore - beforeScore);
                beforeScore = scoreData.getScore();
            }
            else{
                scoreData.setRate(scoreData.getScore());
                beforeScore = scoreData.getScore();
            }

        }
    }

    public skinStatusBoumanResponseDto skinStatusBoumanDummy(UserDetailsImpl userDetails) {

        skinStatusBoumanResponseDto result = new skinStatusBoumanResponseDto();
        LocalDateTime startDatetime;
        LocalDateTime endDatetime;

        startDatetime = LocalDateTime.of(LocalDate.now().minusWeeks(1), LocalTime.of(0,0,0)); //오늘 00:00:00
        endDatetime = LocalDateTime.of(LocalDate.now(), LocalTime.of(23,59,59)); //오늘 23:59:59

        SkinType skinTypeThisWeek = skinTypeRepository.findTopByCreatedAtBetweenAndUser(startDatetime, endDatetime, userDetails.getUser());
        resultAdd(result, skinTypeThisWeek, "일주일전엔 40점이었어요", 40L, 40L, "EFC2C2");


        startDatetime = LocalDateTime.of(LocalDate.now().minusWeeks(2), LocalTime.of(0,0,0)); //오늘 00:00:00
        endDatetime = LocalDateTime.of(LocalDate.now().minusWeeks(1), LocalTime.of(23,59,59)); //오늘 23:59:59

        SkinType skinTypeWeekAgo = skinTypeRepository.findTopByCreatedAtBetweenAndUser(startDatetime, endDatetime, userDetails.getUser());
        resultAdd(result, skinTypeWeekAgo, "한달전엔 60점이었어요", 60L, 20L, "C14242");



        startDatetime = LocalDateTime.of(LocalDate.now().minusMonths(2), LocalTime.of(0,0,0)); //오늘 00:00:00
        endDatetime = LocalDateTime.of(LocalDate.now().minusWeeks(2), LocalTime.of(23,59,59)); //오늘 23:59:59

        SkinType skinTypeMonthAgo= skinTypeRepository.findTopByCreatedAtBetweenAndUser(startDatetime, endDatetime, userDetails.getUser());
        resultAdd(result, skinTypeMonthAgo, "이번주엔 90점이에요", 90L, 30L, "959698");

        return result;
    }

    private void resultAdd(skinStatusBoumanResponseDto result, SkinType skinTypeThisWeek, String msg, Long score, Long rate, String color) {
        //if(skinTypeThisWeek == null)
          //  return;
        result.getAquaScore().add(new ScoreData(msg, score, rate, color));
        result.getOilScore().add(new ScoreData(msg, score, rate, color));
        result.getSensitiveScore().add(new ScoreData(msg, score, rate, color));
        result.getWinkleScore().add(new ScoreData(msg, score, rate, color));
        result.getPigmentScore().add(new ScoreData(msg, score, rate, color));
    }

    public Page<SkinStatusRecordResponseDto> skinStatusRecord(UserDetailsImpl userDetails, Long page) {
        PageRequest pageRequest = PageRequest.of(page.intValue(), 7);

        Page<SkinStatusRecordResponseDto> result = skinTypeRepository.findskinStatusRecord(userDetails, pageRequest);
        for (SkinStatusRecordResponseDto skinStatusRecordResponseDto : result) {
            String date = skinStatusRecordResponseDto.getDate();
            date = date.split(" ")[0];
            String year = date.split("-")[0];
            String month = date.split("-")[1];
            String day = date.split("-")[2];

            DayOfWeek dayOfWeek = LocalDate.of(Integer.parseInt(year), Integer.parseInt(month), Integer.parseInt(day)).getDayOfWeek();
            System.out.println("dayOfWeek = " + dayOfWeek);

            String week = null;
            if(dayOfWeek.equals(DayOfWeek.MONDAY)){
                week = "월";
            }
            else if(dayOfWeek.equals(DayOfWeek.TUESDAY)){
                week = "화";
            }
            else if (dayOfWeek.equals(DayOfWeek.WEDNESDAY)){
                week = "수";
            }
            else if (dayOfWeek.equals(DayOfWeek.THURSDAY)){
                week = "목";
            }
            else if (dayOfWeek.equals(DayOfWeek.FRIDAY)){
                week = "금";
            }
            else if (dayOfWeek.equals(DayOfWeek.SATURDAY)){
                week = "토";
            }
            else if (dayOfWeek.equals(DayOfWeek.SUNDAY)){
                week = "일";
            }

            skinStatusRecordResponseDto.setDate(month + "월 " + day + "일 "+ week+"요일");
            skinStatusRecordResponseDto.setScore(skinStatusRecordResponseDto.getScore() + "점");
        }

        return result;
    }

    class ScoreDataComparator implements Comparator<ScoreData> {
        @Override
        public  int compare(ScoreData a, ScoreData b){
            if(a == null){
                return 0;
            }
            if (b == null){
                return 0;
            }

            if(a.getScore()>b.getScore()) return 1;
            if(a.getScore()<b.getScore()) return -1;
            return 0;
        }
    }


}
