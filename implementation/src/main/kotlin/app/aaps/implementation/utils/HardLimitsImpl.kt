package app.aaps.implementation.utils

import android.content.Context
import app.aaps.annotations.OpenForTesting
import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.resources.ResourceHelper
import app.aaps.core.interfaces.sharedPreferences.SP
import app.aaps.core.interfaces.ui.UiInteraction
import app.aaps.core.interfaces.utils.HardLimits
import app.aaps.database.impl.AppRepository
import app.aaps.database.impl.transactions.InsertTherapyEventAnnouncementTransaction
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

@OpenForTesting
@Singleton
class HardLimitsImpl @Inject constructor(
    private val aapsLogger: AAPSLogger,
    private val uiInteraction: UiInteraction,
    private val sp: SP,
    private val rh: ResourceHelper,
    private val context: Context,
    private val repository: AppRepository,
) : HardLimits {

    private val disposable = CompositeDisposable()

    companion object {

        private const val CHILD = 0
        private const val TEENAGE = 1
        private const val ADULT = 2
        private const val RESISTANT_ADULT = 3
        private const val PREGNANT = 4
        private val MAX_BOLUS = doubleArrayOf(5.0, 10.0, 17.0, 25.0, 60.0)

        // Very Hard Limits Ranges
        // First value is the Lowest and second value is the Highest a Limit can define
        val VERY_HARD_LIMIT_MIN_BG = doubleArrayOf(70.0, 250.0)
        val VERY_HARD_LIMIT_MAX_BG = doubleArrayOf(70.0, 250.0)
        val VERY_HARD_LIMIT_TARGET_BG = doubleArrayOf(70.0, 250.0)

        // Very Hard Limits Ranges for Temp Targets
        val VERY_HARD_LIMIT_TEMP_MIN_BG = intArrayOf(70, 180)
        val VERY_HARD_LIMIT_TEMP_MAX_BG = intArrayOf(70, 270)
        val VERY_HARD_LIMIT_TEMP_TARGET_BG = intArrayOf(70, 200)
        val MIN_DIA = doubleArrayOf(5.0, 5.0, 5.0, 5.0, 5.0)
        val MAX_DIA = doubleArrayOf(9.0, 9.0, 9.0, 9.0, 10.0)
        val MIN_IC = doubleArrayOf(2.0, 2.0, 2.0, 2.0, 0.3)
        val MAX_IC = doubleArrayOf(100.0, 100.0, 100.0, 100.0, 100.0)
        const val MIN_ISF = 2.0 // mgdl
        const val MAX_ISF = 1000.0 // mgdl
        val MAX_IOB_AMA = doubleArrayOf(3.0, 5.0, 7.0, 12.0, 25.0)
        val MAX_IOB_SMB = doubleArrayOf(7.0, 13.0, 22.0, 30.0, 70.0)
        val MAX_BASAL = doubleArrayOf(2.0, 5.0, 10.0, 12.0, 25.0)

        //LGS Hard limits
        //No IOB at all
        const val MAX_IOB_LGS = 0.0

    }

    private fun loadAge(): Int = when (sp.getString(app.aaps.core.utils.R.string.key_age, "")) {
        rh.gs(app.aaps.core.utils.R.string.key_child)          -> CHILD
        rh.gs(app.aaps.core.utils.R.string.key_teenage)        -> TEENAGE
        rh.gs(app.aaps.core.utils.R.string.key_adult)          -> ADULT
        rh.gs(app.aaps.core.utils.R.string.key_resistantadult) -> RESISTANT_ADULT
        rh.gs(app.aaps.core.utils.R.string.key_pregnant)       -> PREGNANT
        else                                                   -> ADULT
    }

    override fun maxBolus(): Double = MAX_BOLUS[loadAge()]
    override fun maxIobAMA(): Double = MAX_IOB_AMA[loadAge()]
    override fun maxIobSMB(): Double = MAX_IOB_SMB[loadAge()]
    override fun maxBasal(): Double = MAX_BASAL[loadAge()]
    override fun minDia(): Double = MIN_DIA[loadAge()]
    override fun maxDia(): Double = MAX_DIA[loadAge()]
    override fun minIC(): Double = MIN_IC[loadAge()]
    override fun maxIC(): Double = MAX_IC[loadAge()]

    // safety checks
    override fun checkHardLimits(value: Double, valueName: Int, lowLimit: Double, highLimit: Double): Boolean =
        value == verifyHardLimits(value, valueName, lowLimit, highLimit)

    override fun isInRange(value: Double, lowLimit: Double, highLimit: Double): Boolean =
        value in lowLimit..highLimit

    override fun verifyHardLimits(value: Double, valueName: Int, lowLimit: Double, highLimit: Double): Double {
        var newValue = value
        if (newValue < lowLimit || newValue > highLimit) {
            newValue = max(newValue, lowLimit)
            newValue = min(newValue, highLimit)
            var msg = rh.gs(app.aaps.core.ui.R.string.valueoutofrange, rh.gs(valueName))
            msg += ".\n"
            msg += rh.gs(app.aaps.core.ui.R.string.valuelimitedto, value, newValue)
            aapsLogger.error(msg)
            disposable += repository.runTransaction(InsertTherapyEventAnnouncementTransaction(msg)).subscribe()
            uiInteraction.showToastAndNotification(context, msg, app.aaps.core.ui.R.raw.error)
        }
        return newValue
    }
}