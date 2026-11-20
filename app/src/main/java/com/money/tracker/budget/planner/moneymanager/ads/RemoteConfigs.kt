package com.money.tracker.budget.planner.moneymanager.ads

import com.cscmobi.libraryads.commons.remote.CSCKonfigModel
import com.cscmobi.libraryads.commons.remote.konfig

object RemoteConfigs : CSCKonfigModel {
    val check_organic_user by konfig("check_organic_user", true)
    val native_collap by konfig("native_collap", true)
    val native_banner by konfig("native_banner", true)
    val native_all by konfig("native_all", true)
    val inter_all by konfig("inter_all", true)
    val adjustAppToken by konfig("adjustAppToken", "")
}
