package com.back.global.s3

import com.back.global.exception.ErrorCode
import com.back.global.exception.ServiceException
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.Delete
import software.amazon.awssdk.services.s3.model.ObjectIdentifier
import software.amazon.awssdk.services.s3.model.S3Exception
import java.io.IOException
import java.net.URL
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.UUID

// S3를 이용한 이미지 파일 업로드 및 삭제(단건/다건) 처리
@Component
class S3ImageService(
    private val s3Client: S3Client,
    @Value("\${spring.cloud.aws.s3.bucket}")
    private val bucketName: String
) {
    /**
     * 단일 이미지를 S3 버킷에 업로드
     *
     * @param multipartFile 업로드할 이미지 파일 객체
     * @return S3에 저장된 이미지의 접근 URL (파일이 비어있거나 null일 경우 null 반환)
     */
    fun upload(multipartFile: MultipartFile?): String? {
        // 파일이 존재하지 않거나 비어있는 경우 업로드 생략
        if (multipartFile == null || multipartFile.isEmpty) {
            return null
        }

        // 원본 파일명을 추출하며, 알 수 없는 경우 "unknown"을 기본값으로 사용
        val originalFileName = multipartFile.originalFilename ?: "unknown"
        // 파일명 중복으로 인한 덮어쓰기를 방지하기 위해 UUID를 결합하여 고유한 저장용 파일명 생성
        val storeFileName = "${UUID.randomUUID()}-$originalFileName"

        try {
            // S3에 객체를 업로드 (버킷 이름, 저장할 파일명, 컨텐츠 타입을 지정)
            s3Client.putObject(
                { it.bucket(bucketName).key(storeFileName).contentType(multipartFile.contentType) },
                RequestBody.fromInputStream(multipartFile.inputStream, multipartFile.size)
            )
        } catch (e: IOException) {
            throw ServiceException(ErrorCode.IMAGE_UPLOAD_FAILED)
        }

        // 업로드 성공 후 해당 객체에 접근할 수 있는 절대 URL을 생성하여 반환
        return s3Client.utilities()
            .getUrl { it.bucket(bucketName).key(storeFileName) }
            .toString()
    }

    /**
     * S3 버킷에서 단일 이미지 객체를 삭제
     *
     * @param imageUrl 삭제할 이미지의 전체 S3 주소 URL
     */
    fun delete(imageUrl: String) {
        // URL에서 실제 S3 객체 키(경로 및 파일명)를 추출
        val key = getKeyFromImageAddress(imageUrl)

        try {
            // 추출한 키를 기반으로 S3 단건 삭제 요청 수행
            s3Client.deleteObject { it.bucket(bucketName).key(key) }
        } catch (e: S3Exception) {
            throw ServiceException(ErrorCode.IMAGE_DELETE_FAILED)
        }
    }

    /**
     * S3 버킷에서 여러 개의 이미지 객체를 한 번의 요청으로 일괄 삭제
     *
     * @param imageUrls 삭제할 이미지들의 S3 주소 URL 목록
     */
    fun deleteMultiple(imageUrls: List<String>?) {
        // 삭제할 목록이 비어있으면 불필요한 S3 요청을 방지하기 위해 즉시 종료
        if (imageUrls.isNullOrEmpty()) return

        // 각 이미지 URL에서 S3 객체 키를 추출하여 다중 삭제를 위한 식별자(ObjectIdentifier) 리스트로 변환
        val objectIdentifiers = imageUrls.map { imageUrl ->
            ObjectIdentifier.builder().key(getKeyFromImageAddress(imageUrl)).build()
        }

        // 삭제할 식별자들을 담은 Delete 객체 생성
        val delete = Delete.builder()
            .objects(objectIdentifiers)
            .build()

        try {
            // S3 다중 객체 삭제 요청 수행
            s3Client.deleteObjects { it.bucket(bucketName).delete(delete) }
        } catch (e: S3Exception) {
            throw ServiceException(ErrorCode.IMAGE_DELETE_FAILED)
        }
    }

    /**
     * (내부 메서드) 전체 이미지 URL에서 도메인을 제외한 S3 객체의 키 부분만 추출
     * * @param imageUrl 원본 이미지 URL (예: https://bucket.s3.../image.png)
     * @return 디코딩된 S3 객체 키 (맨 앞의 '/' 문자가 제거된 상태)
     */
    private fun getKeyFromImageAddress(imageUrl: String): String {
        return try {
            val url = URL(imageUrl)
            // URL에 포함된 한글이나 특수문자가 인코딩되어 있을 수 있으므로 UTF-8로 디코딩 수행
            val decodingKey = URLDecoder.decode(url.path, StandardCharsets.UTF_8.name())
            // url.path는 '/파일명' 형태로 반환되므로, 맨 앞의 '/'를 제외한 문자열을 키로 사용
            decodingKey.substring(1)
        } catch (e: Exception) {
            throw ServiceException(ErrorCode.INVALID_IMAGE_URL)
        }
    }
}