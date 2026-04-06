import com.ripplehealthcare.bproboard.domain.model.BaseTestResult
import com.ripplehealthcare.bproboard.domain.model.StageResultData
import com.ripplehealthcare.bproboard.domain.model.TestType
import java.util.Date
import java.util.UUID

data class FourStageResult(
    override val testId: String = UUID.randomUUID().toString(),
    override val sessionId: String = "",
    override val patientId: String = "",
    override val centerId: String = "", // Added
    override val doctorId: String = "",  // Replaces userId
    override val timestamp: Date = Date(),
    override val testType: TestType = TestType.FOUR_STAGE_BALANCE,

    val stages: List<StageResultData> = emptyList()
): BaseTestResult()