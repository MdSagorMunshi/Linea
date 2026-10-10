package com.ryanshelby.linea.ui.screens.settings.blocking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ryanshelby.linea.data.local.dao.CountryRuleDao
import com.ryanshelby.linea.data.local.entities.CountryRuleEntity
import com.ryanshelby.linea.data.local.entities.CountryRuleType
import com.ryanshelby.linea.data.preferences.LineaPreferences
import com.ryanshelby.linea.telecom.screening.CountryDetails
import com.ryanshelby.linea.telecom.screening.CountryDetector
import com.ryanshelby.linea.telecom.screening.SimCountryDetector
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CountryBlockingUiState(
    val isEnabled: Boolean = true,
    val filterMode: String = "BLACKLIST_ONLY", // "BLACKLIST_ONLY" vs "WHITELIST_ONLY"
    val whitelistOverrides: Boolean = true,
    val exemptContacts: Boolean = true,
    val blacklistedCountries: List<CountryRuleEntity> = emptyList(),
    val whitelistedCountries: List<CountryRuleEntity> = emptyList(),
    val allCatalogCountries: List<CountryDetails> = emptyList(),
    val selectedTab: Int = 0, // 0 = Blacklist, 1 = Whitelist
    val searchQuery: String = "",
    val userHomeIso: String = "US"
)

@HiltViewModel
class CountryBlockingViewModel @Inject constructor(
    private val countryRuleDao: CountryRuleDao,
    private val preferences: LineaPreferences
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(0)
    private val _searchQuery = MutableStateFlow("")
    private val catalogCountries = CountryDetector.getAllCatalogCountries()

    val uiState: StateFlow<CountryBlockingUiState> = combine(
        countryRuleDao.getAllRules(),
        preferences.countryBlockingEnabled,
        preferences.countryFilterMode,
        preferences.countryWhitelistOverrides,
        preferences.countryExemptContacts,
        _selectedTab,
        _searchQuery
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        val rules = args[0] as List<CountryRuleEntity>
        val isEnabled = args[1] as Boolean
        val mode = args[2] as String
        val whitelistOverrides = args[3] as Boolean
        val exemptContacts = args[4] as Boolean
        val tab = args[5] as Int
        val query = args[6] as String

        val blacklist = rules.filter { it.ruleType == CountryRuleType.BLACKLIST }
        val whitelist = rules.filter { it.ruleType == CountryRuleType.WHITELIST }

        CountryBlockingUiState(
            isEnabled = isEnabled,
            filterMode = mode,
            whitelistOverrides = whitelistOverrides,
            exemptContacts = exemptContacts,
            blacklistedCountries = blacklist,
            whitelistedCountries = whitelist,
            allCatalogCountries = catalogCountries,
            selectedTab = tab,
            searchQuery = query,
            userHomeIso = SimCountryDetector.currentCountryIso
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CountryBlockingUiState(allCatalogCountries = catalogCountries)
    )

    fun setSelectedTab(tab: Int) {
        _selectedTab.value = tab
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleCountryBlocking(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setCountryBlockingEnabled(enabled)
        }
    }

    fun setFilterMode(mode: String) {
        viewModelScope.launch {
            preferences.setCountryFilterMode(mode)
        }
    }

    fun toggleWhitelistOverrides(overrides: Boolean) {
        viewModelScope.launch {
            preferences.setCountryWhitelistOverrides(overrides)
        }
    }

    fun toggleExemptContacts(exempt: Boolean) {
        viewModelScope.launch {
            preferences.setCountryExemptContacts(exempt)
        }
    }

    fun addCountryRule(
        country: CountryDetails,
        ruleType: CountryRuleType,
        appliesToAllSharedCode: Boolean = false
    ) {
        viewModelScope.launch {
            countryRuleDao.insertRule(
                CountryRuleEntity(
                    countryIso = country.countryIso,
                    countryName = country.countryName,
                    dialCode = country.dialCode,
                    flagEmoji = country.flagEmoji,
                    ruleType = ruleType,
                    isEnabled = true,
                    appliesToAllSharedCode = appliesToAllSharedCode
                )
            )
        }
    }

    fun removeCountryRule(ruleId: Long) {
        viewModelScope.launch {
            countryRuleDao.deleteRuleById(ruleId)
        }
    }

    fun removeCountryRuleByIso(countryIso: String, ruleType: CountryRuleType) {
        viewModelScope.launch {
            countryRuleDao.deleteRuleByIsoAndType(countryIso, ruleType)
        }
    }

    fun toggleRuleEnabled(rule: CountryRuleEntity) {
        viewModelScope.launch {
            countryRuleDao.updateRule(rule.copy(isEnabled = !rule.isEnabled))
        }
    }
}
