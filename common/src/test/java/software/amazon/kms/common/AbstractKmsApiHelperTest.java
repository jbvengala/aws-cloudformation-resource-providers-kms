package software.amazon.kms.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;
import static software.amazon.kms.common.KeyApiHelper.ACCESS_DENIED_ERROR_CODE;
import static software.amazon.kms.common.KeyApiHelper.THROTTLING_ERROR_CODE;
import static software.amazon.kms.common.KeyApiHelper.VALIDATION_ERROR_CODE;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.kms.model.AlreadyExistsException;
import software.amazon.awssdk.services.kms.model.DependencyTimeoutException;
import software.amazon.awssdk.services.kms.model.DisabledException;
import software.amazon.awssdk.services.kms.model.InvalidAliasNameException;
import software.amazon.awssdk.services.kms.model.InvalidArnException;
import software.amazon.awssdk.services.kms.model.InvalidMarkerException;
import software.amazon.awssdk.services.kms.model.KmsException;
import software.amazon.awssdk.services.kms.model.KmsInternalException;
import software.amazon.awssdk.services.kms.model.KmsInvalidStateException;
import software.amazon.awssdk.services.kms.model.LimitExceededException;
import software.amazon.awssdk.services.kms.model.MalformedPolicyDocumentException;
import software.amazon.awssdk.services.kms.model.NotFoundException;
import software.amazon.awssdk.services.kms.model.TagException;
import software.amazon.awssdk.services.kms.model.UnsupportedOperationException;
import software.amazon.cloudformation.exceptions.CfnAccessDeniedException;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInternalFailureException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.exceptions.CfnServiceLimitExceededException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;
import software.amazon.cloudformation.exceptions.CfnUnauthorizedTaggingOperationException;

public class AbstractKmsApiHelperTest {
    private static final String TAG_ON_CREATE_ERROR = "An error occurred (AccessDeniedException) " +
            "when calling the CreateKey operation: You don't have the kms:TagResource permission " +
            "that is required to add tags during key creation.";
    private static final String UNTAG_ERROR = "An error occurred (AccessDeniedException) " +
            "when calling the UntagResource operation: User: arn:aws:sts::170477759626:assumed-role/NoTagging " +
            "is not authorized to perform: kms:UntagResource on resource: " +
            "arn:aws:kms:us-east-1:170477759626:key/d1e4b07c-f54e-459d-929a-9751c4b44262 " +
            "because no identity-based policy allows the kms:UntagResource action";
    private static final String LIST_RESOURCE_TAGS_ERROR = "An error occurred (AccessDeniedException) " +
            "when calling the UntagResource operation: User: arn:aws:sts::170477759626:assumed-role/NoTagging " +
            "is not authorized to perform: kms:ListResourceTags on resource: " +
            "arn:aws:kms:us-east-1:170477759626:key/d1e4b07c-f54e-459d-929a-9751c4b44262 " +
            "because no identity-based policy allows the kms:ListResourceTags action";

    private MockKmsApiHelper mockKmsApiHelper;

    @BeforeEach
    public void setup() {
        mockKmsApiHelper = new MockKmsApiHelper();
    }

    @Test
    public void testAlreadyExists() {
        assertThatExceptionOfType(CfnAlreadyExistsException.class)
            .isThrownBy(() -> mockKmsApiHelper.testExceptionWrapping(AlreadyExistsException.class));
    }

    @Test
    public void testInternalFailure() {
        assertThatExceptionOfType(CfnInternalFailureException.class)
            .isThrownBy(() -> mockKmsApiHelper.testExceptionWrapping(InvalidMarkerException.class));
    }

    @Test
    public void testLimitExceeded() {
        assertThatExceptionOfType(CfnServiceLimitExceededException.class)
            .isThrownBy(() -> mockKmsApiHelper.testExceptionWrapping(LimitExceededException.class));
    }

    @ParameterizedTest
    @ValueSource(classes = {InvalidAliasNameException.class, KmsInvalidStateException.class,
        InvalidArnException.class, MalformedPolicyDocumentException.class, TagException.class,
        UnsupportedOperationException.class,
        DisabledException.class})
    public void testInvalidRequest(final Class<? extends RuntimeException> kmsException) {
        assertThatExceptionOfType(CfnInvalidRequestException.class)
            .isThrownBy(() -> mockKmsApiHelper.testExceptionWrapping(kmsException));
    }

    @ParameterizedTest
    @ValueSource(classes = {KmsInternalException.class, DependencyTimeoutException.class})
    public void testServiceInternalError(final Class<? extends RuntimeException> kmsException) {
        assertThatExceptionOfType(CfnServiceInternalErrorException.class)
            .isThrownBy(() -> mockKmsApiHelper.testExceptionWrapping(kmsException));
    }

    @Test
    public void testNotFound() {
        assertThatExceptionOfType(CfnNotFoundException.class)
            .isThrownBy(() -> mockKmsApiHelper.testExceptionWrapping(NotFoundException.class));
    }

    @Test
    public void testThrottling() {
        final AwsServiceException throttlingException = KmsException.builder().awsErrorDetails(
            AwsErrorDetails.builder()
                .sdkHttpResponse(SdkHttpResponse.builder()
                    .statusCode(400)
                    .build())
                .errorCode(THROTTLING_ERROR_CODE)
                .build())
            .build();

        assertThatExceptionOfType(CfnThrottlingException.class)
            .isThrownBy(() -> mockKmsApiHelper.testExceptionWrapping(throttlingException));
    }

    @Test
    public void testAccessDenied() {
        final AwsServiceException accessDeniedException = KmsException.builder().awsErrorDetails(
            AwsErrorDetails.builder()
                .sdkHttpResponse(SdkHttpResponse.builder()
                    .statusCode(400)
                    .build())
                .errorCode(ACCESS_DENIED_ERROR_CODE)
                .build())
            .build();

        assertThatExceptionOfType(CfnAccessDeniedException.class)
            .isThrownBy(() -> mockKmsApiHelper.testExceptionWrapping(accessDeniedException));
    }

    @Test
    public void testTagOnCreateAccessDenied() {
        final AwsServiceException accessDeniedException = KmsException.builder().awsErrorDetails(
                AwsErrorDetails.builder()
                        .sdkHttpResponse(SdkHttpResponse.builder()
                                .statusCode(400)
                                .build())
                        .errorCode(ACCESS_DENIED_ERROR_CODE)
                        .errorMessage(TAG_ON_CREATE_ERROR)
                        .build())
                .build();

        assertThatExceptionOfType(CfnUnauthorizedTaggingOperationException.class)
                .isThrownBy(() -> mockKmsApiHelper.testExceptionWrapping(accessDeniedException));
    }

    @Test
    public void testTaggingAccessDenied() {
        final AwsServiceException accessDeniedException = KmsException.builder().awsErrorDetails(
                AwsErrorDetails.builder()
                        .sdkHttpResponse(SdkHttpResponse.builder()
                                .statusCode(400)
                                .build())
                        .errorCode(ACCESS_DENIED_ERROR_CODE)
                        .errorMessage(UNTAG_ERROR)
                        .build())
                .build();

        assertThatExceptionOfType(CfnUnauthorizedTaggingOperationException.class)
                .isThrownBy(() -> mockKmsApiHelper.testExceptionWrapping(accessDeniedException));
    }

    @Test
    public void testListResourceTagsAccessDenied() {
        final AwsServiceException accessDeniedException = KmsException.builder().awsErrorDetails(
                AwsErrorDetails.builder()
                        .sdkHttpResponse(SdkHttpResponse.builder()
                                .statusCode(400)
                                .build())
                        .errorCode(ACCESS_DENIED_ERROR_CODE)
                        .errorMessage(LIST_RESOURCE_TAGS_ERROR)
                        .build())
                .build();

        assertThatExceptionOfType(CfnUnauthorizedTaggingOperationException.class)
                .isThrownBy(() -> mockKmsApiHelper.testExceptionWrapping(accessDeniedException));
    }

    @Test
    public void testValidationException() {
        final AwsServiceException validationException = KmsException.builder().awsErrorDetails(
            AwsErrorDetails.builder()
                .sdkHttpResponse(SdkHttpResponse.builder()
                    .statusCode(400)
                    .build())
                .errorCode(VALIDATION_ERROR_CODE)
                .build())
            .build();

        assertThatExceptionOfType(CfnInvalidRequestException.class)
            .isThrownBy(() -> mockKmsApiHelper.testExceptionWrapping(validationException));

    }

    @Test
    public void testGeneralKmsException() {
        final AwsServiceException generalKmsException = KmsException.builder()
            .awsErrorDetails(AwsErrorDetails.builder().build()).build();

        assertThatExceptionOfType(CfnGeneralServiceException.class)
            .isThrownBy(() -> mockKmsApiHelper.testExceptionWrapping(generalKmsException));
    }

    @Test
    public void testAddMessageIfNull() {
        final MalformedPolicyDocumentException malformedPolicyDocumentException = MalformedPolicyDocumentException.builder()
                .message("null (Service: Kms, Status Code: 400, Request ID: null, Extended Request ID: null)")
                .build();

        try {
            mockKmsApiHelper.testExceptionWrapping(malformedPolicyDocumentException);
            fail("Expected CfnInvalidRequestException");
        } catch (final CfnInvalidRequestException e) {
            assertThat(e.getMessage()).isEqualTo("MockOperation failed due to MalformedPolicyDocumentException " +
                    "(Service: Kms, Status Code: 400, Request ID: null, Extended Request ID: null)");
        }
    }
}
