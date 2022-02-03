package com.ncores.plaluvs.service;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.ncores.plaluvs.domain.Photo;
import com.ncores.plaluvs.domain.skintype.SkinType;
import com.ncores.plaluvs.domain.user.User;
import com.ncores.plaluvs.exception.ErrorCode;
import com.ncores.plaluvs.exception.PlaluvsException;
import com.ncores.plaluvs.repository.PhotoRepository;
import com.ncores.plaluvs.repository.skinType.SkinTypeRepository;
import com.ncores.plaluvs.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.transaction.Transactional;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PhotoService {
    private final AmazonS3Client amazonS3Client;
    private final PhotoRepository photoRepository;
    private final SkinTypeRepository skinTypeRepository;

    @Value("${cloud.aws.s3.bucket}")
    public String bucket;  // S3 버킷 이름

    public Photo upload(MultipartFile multipartFile, String dirName, UserDetailsImpl userDetails) throws IOException {
        File uploadFile = convert(multipartFile)  // 파일 변환할 수 없으면 에러
                .orElseThrow(() -> new IllegalArgumentException("error: MultipartFile -> File convert fail"));

        String uploadURL = upload(uploadFile, dirName);
        Photo photo = new Photo(userDetails.getUser(), multipartFile.getOriginalFilename(), uploadURL);
        Photo save = photoRepository.save(photo);

        return save;
    }

    // S3로 파일 업로드하기
    private String upload(File uploadFile, String dirName) {
        String fileName = dirName + "/" + UUID.randomUUID() + uploadFile.getName();   // S3에 저장된 파일 이름
        String uploadImageUrl = putS3(uploadFile, fileName); // s3로 업로드
        removeNewFile(uploadFile);


        return uploadImageUrl;
    }

    // S3로 업로드
    private String putS3(File uploadFile, String fileName) {
        amazonS3Client.putObject(new PutObjectRequest(bucket, fileName, uploadFile).withCannedAcl(CannedAccessControlList.PublicRead));
        return amazonS3Client.getUrl(bucket, fileName).toString();
    }

    // 로컬에 저장된 이미지 지우기
    private void removeNewFile(File targetFile) {
        if (targetFile.delete()) {
            log.info("File delete success");
            return;
        }
        log.info("File delete fail");
    }

    // 로컬에 파일 업로드 하기
    private Optional<File> convert(MultipartFile file) throws IOException {
        File convertFile = new File(System.getProperty("user.dir") + "/" + file.getOriginalFilename());
        if (convertFile.createNewFile()) { // 바로 위에서 지정한 경로에 File이 생성됨 (경로가 잘못되었다면 생성 불가능)
            try (FileOutputStream fos = new FileOutputStream(convertFile)) { // FileOutputStream 데이터를 파일에 바이트 스트림으로 저장하기 위함
                fos.write(file.getBytes());
            }
            return Optional.of(convertFile);
        }

        return Optional.empty();
    }

    @Transactional
    public Photo makeGetImageData(Photo photo) throws PlaluvsException {
        String result = makeGetAPI(photo.getStored_file_path());

        if(result.equals("-1"))
            throw new PlaluvsException(ErrorCode.PHOTO_FACE_EMPTY);
        else if(result.equals("-2"))
            throw new PlaluvsException(ErrorCode.PHOTO_FACE_MANY);

        String[]  resultList = result.split("\n");
        log.info("resultList = {}", resultList);

        String nose = resultList[0].split(" : ")[1];
        String leftCheck = resultList[1].split(" : ")[1];
        String rightCheck = resultList[2].split(" : ")[1];

        Long noseScore = Long.valueOf((long) ((7 - Double.valueOf(nose)) * 100 /7));
        Long leftScore = Long.valueOf((long) ((7 - Double.valueOf(leftCheck)) * 100 /7));
        Long rightScore = Long.valueOf((long) ((7 - Double.valueOf(rightCheck)) * 100 /7));

        photo.setNoseTrouble(noseScore);
        photo.setLeftCheckTrouble(leftScore);
        photo.setRightCheckTrouble(rightScore);


        return photo;
    }
    private String makeGetAPI(String url) {
        String pythonURL = "http://localhost:5000/image?url=";
        List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>();
        converters.add(new FormHttpMessageConverter());
        converters.add(new StringHttpMessageConverter());

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setMessageConverters(converters);

        // parameter 세팅
        MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
        map.add("str", "thisistest");

        String resultURL = pythonURL + url;
        log.info(resultURL);

        // REST API 호출
        String result = restTemplate.getForObject(resultURL, String.class);


        return result;
    }

    @Transactional
    public void setSkinStatus(User user, Photo result) throws PlaluvsException {
        SkinType dailySkinType = skinTypeRepository.findDailySkinTypeException(user);

        long skinTypeScore = dailySkinType.getScore() * 60 / 100;
        long photoScore = (result.getNoseTrouble() + result.getLeftCheckTrouble() + result.getRightCheckTrouble()) * 40 /300;

        dailySkinType.setTotalScore( skinTypeScore + photoScore );
    }
}
