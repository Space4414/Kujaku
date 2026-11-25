package ani.dantotsu.notifications

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import java.util.concurrent.TimeUnit

object UpdateCheckScheduler {
    private val intervalsHours = listOf(3L, 6L, 12L, 24L)

    fun schedule(context: Context) {
        if (!PrefManager.getVal<Boolean>(PrefName.CheckUpdate)) {
            cancel(context)
            return
        }
        val selectedIndex = PrefManager.getVal<Int>(PrefName.UpdateCheckInterval)
            .coerceIn(intervalsHours.indices)
        val request = PeriodicWorkRequestBuilder<UpdateCheckWorker>(
            intervalsHours[selectedIndex], TimeUnit.HOURS
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UpdateCheckWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UpdateCheckWorker.WORK_NAME)
    }

    fun intervals(): List<Long> = intervalsHours
}
