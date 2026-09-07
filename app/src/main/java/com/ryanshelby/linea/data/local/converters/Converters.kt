package com.ryanshelby.linea.data.local.converters

import androidx.room.TypeConverter
import com.ryanshelby.linea.data.local.entities.BlockAction
import com.ryanshelby.linea.data.local.entities.BlockMatchType
import com.ryanshelby.linea.data.local.entities.CallDirectionType
import com.ryanshelby.linea.data.local.entities.ReminderStatus
import com.ryanshelby.linea.data.local.entities.RuleAction
import com.ryanshelby.linea.data.local.entities.RuleAllowedFilter

class Converters {

    @TypeConverter
    fun fromCallDirectionType(value: CallDirectionType?): String? = value?.name

    @TypeConverter
    fun toCallDirectionType(value: String?): CallDirectionType? =
        value?.let { enumValueOf<CallDirectionType>(it) }

    @TypeConverter
    fun fromBlockMatchType(value: BlockMatchType?): String? = value?.name

    @TypeConverter
    fun toBlockMatchType(value: String?): BlockMatchType? =
        value?.let { enumValueOf<BlockMatchType>(it) }

    @TypeConverter
    fun fromBlockAction(value: BlockAction?): String? = value?.name

    @TypeConverter
    fun toBlockAction(value: String?): BlockAction? =
        value?.let { enumValueOf<BlockAction>(it) }

    @TypeConverter
    fun fromRuleAllowedFilter(value: RuleAllowedFilter?): String? = value?.name

    @TypeConverter
    fun toRuleAllowedFilter(value: String?): RuleAllowedFilter? =
        value?.let { enumValueOf<RuleAllowedFilter>(it) }

    @TypeConverter
    fun fromRuleAction(value: RuleAction?): String? = value?.name

    @TypeConverter
    fun toRuleAction(value: String?): RuleAction? =
        value?.let { enumValueOf<RuleAction>(it) }

    @TypeConverter
    fun fromReminderStatus(value: ReminderStatus?): String? = value?.name

    @TypeConverter
    fun toReminderStatus(value: String?): ReminderStatus? =
        value?.let { enumValueOf<ReminderStatus>(it) }
}
