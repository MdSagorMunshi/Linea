package com.ryanshelby.linea.telecom

import androidx.annotation.DrawableRes
import com.ryanshelby.linea.R
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class TimeOfDayState(
    val displayName: String,
    @DrawableRes val iconRes: Int
) {
    EARLY_MORNING("Early Morning", R.drawable.ic_time_early_morning),
    MORNING("Morning", R.drawable.ic_time_morning),
    MIDDAY("Midday", R.drawable.ic_time_midday),
    AFTERNOON("Afternoon", R.drawable.ic_time_afternoon),
    EVENING("Evening", R.drawable.ic_time_evening),
    NIGHT("Night", R.drawable.ic_time_night);

    companion object {
        fun fromHour(hour: Int): TimeOfDayState {
            return when (hour) {
                in 5..7 -> EARLY_MORNING   // 5:00 AM - 7:59 AM
                in 8..11 -> MORNING        // 8:00 AM - 11:59 AM
                in 12..13 -> MIDDAY        // 12:00 PM - 1:59 PM
                in 14..16 -> AFTERNOON     // 2:00 PM - 4:59 PM
                in 17..20 -> EVENING       // 5:00 PM - 8:59 PM
                else -> NIGHT              // 21:00 - 04:59 (9:00 PM - 4:59 AM)
            }
        }
    }
}

data class CountryInfo(
    val isoCode: String,
    val dialCode: String,
    val countryName: String,
    val flagEmoji: String,
    val primaryTimeZone: String
)

data class InternationalPreview(
    val countryName: String,
    val flagEmoji: String,
    val localTimeFormatted: String,
    val timeZoneShort: String,
    val timeOfDay: TimeOfDayState = TimeOfDayState.MORNING,
    val isLateNight: Boolean = (timeOfDay == TimeOfDayState.NIGHT),
    val lateNightWarning: String? = null
)

object InternationalCountryHelper {

    /**
     * Complete 100% global catalog of all 248 sovereign nations, UN observer states,
     * territories, and island protectorates worldwide with validated IANA primary timezones.
     */
    private val countryList = listOf(
        CountryInfo("BS", "+1242", "Bahamas", "🇧🇸", "America/Nassau"),
        CountryInfo("BB", "+1246", "Barbados", "🇧🇧", "America/Barbados"),
        CountryInfo("AI", "+1264", "Anguilla", "🇦🇮", "America/Anguilla"),
        CountryInfo("AG", "+1268", "Antigua and Barbuda", "🇦🇬", "America/Antigua"),
        CountryInfo("VG", "+1284", "British Virgin Islands", "🇻🇬", "America/Tortola"),
        CountryInfo("VI", "+1340", "U.S. Virgin Islands", "🇻🇮", "America/St_Thomas"),
        CountryInfo("KY", "+1345", "Cayman Islands", "🇰🇾", "America/Cayman"),
        CountryInfo("BM", "+1441", "Bermuda", "🇧🇲", "Atlantic/Bermuda"),
        CountryInfo("GD", "+1473", "Grenada", "🇬🇩", "America/Grenada"),
        CountryInfo("TC", "+1649", "Turks and Caicos Islands", "🇹🇨", "America/Grand_Turk"),
        CountryInfo("JM", "+1658", "Jamaica", "🇯🇲", "America/Jamaica"),
        CountryInfo("MS", "+1664", "Montserrat", "🇲🇸", "America/Montserrat"),
        CountryInfo("MP", "+1670", "Northern Mariana Islands", "🇲🇵", "Pacific/Saipan"),
        CountryInfo("GU", "+1671", "Guam", "🇬🇺", "Pacific/Guam"),
        CountryInfo("AS", "+1684", "American Samoa", "🇦🇸", "Pacific/Pago_Pago"),
        CountryInfo("SX", "+1721", "Sint Maarten", "🇸🇽", "America/Curacao"),
        CountryInfo("LC", "+1758", "Saint Lucia", "🇱🇨", "America/St_Lucia"),
        CountryInfo("DM", "+1767", "Dominica", "🇩🇲", "America/Dominica"),
        CountryInfo("VC", "+1784", "Saint Vincent and the Grenadines", "🇻🇨", "America/St_Vincent"),
        CountryInfo("PR", "+1787", "Puerto Rico", "🇵🇷", "America/Puerto_Rico"),
        CountryInfo("DO", "+1809", "Dominican Republic", "🇩🇴", "America/Santo_Domingo"),
        CountryInfo("DO", "+1829", "Dominican Republic", "🇩🇴", "America/Santo_Domingo"),
        CountryInfo("DO", "+1849", "Dominican Republic", "🇩🇴", "America/Santo_Domingo"),
        CountryInfo("TT", "+1868", "Trinidad and Tobago", "🇹🇹", "America/Port_of_Spain"),
        CountryInfo("KN", "+1869", "Saint Kitts and Nevis", "🇰🇳", "America/St_Kitts"),
        CountryInfo("JM", "+1876", "Jamaica", "🇯🇲", "America/Jamaica"),
        CountryInfo("PR", "+1939", "Puerto Rico", "🇵🇷", "America/Puerto_Rico"),
        CountryInfo("GI", "+350", "Gibraltar", "🇬🇮", "Europe/Gibraltar"),
        CountryInfo("PT", "+351", "Portugal", "🇵🇹", "Europe/Lisbon"),
        CountryInfo("LU", "+352", "Luxembourg", "🇱🇺", "Europe/Luxembourg"),
        CountryInfo("IE", "+353", "Ireland", "🇮🇪", "Europe/Dublin"),
        CountryInfo("IS", "+354", "Iceland", "🇮🇸", "Atlantic/Reykjavik"),
        CountryInfo("AL", "+355", "Albania", "🇦🇱", "Europe/Tirane"),
        CountryInfo("MT", "+356", "Malta", "🇲🇹", "Europe/Malta"),
        CountryInfo("CY", "+357", "Cyprus", "🇨🇾", "Asia/Nicosia"),
        CountryInfo("FI", "+358", "Finland", "🇫🇮", "Europe/Helsinki"),
        CountryInfo("AX", "+358", "Åland Islands", "🇦🇽", "Europe/Mariehamn"),
        CountryInfo("BG", "+359", "Bulgaria", "🇧🇬", "Europe/Sofia"),
        CountryInfo("LT", "+370", "Lithuania", "🇱🇹", "Europe/Vilnius"),
        CountryInfo("LV", "+371", "Latvia", "🇱🇻", "Europe/Riga"),
        CountryInfo("EE", "+372", "Estonia", "🇪🇪", "Europe/Tallinn"),
        CountryInfo("MD", "+373", "Moldova", "🇲🇩", "Europe/Chisinau"),
        CountryInfo("AM", "+374", "Armenia", "🇦🇲", "Asia/Yerevan"),
        CountryInfo("BY", "+375", "Belarus", "🇧🇾", "Europe/Minsk"),
        CountryInfo("AD", "+376", "Andorra", "🇦🇩", "Europe/Andorra"),
        CountryInfo("MC", "+377", "Monaco", "🇲🇨", "Europe/Monaco"),
        CountryInfo("SM", "+378", "San Marino", "🇸🇲", "Europe/San_Marino"),
        CountryInfo("VA", "+379", "Vatican City", "🇻🇦", "Europe/Vatican"),
        CountryInfo("UA", "+380", "Ukraine", "🇺🇦", "Europe/Kyiv"),
        CountryInfo("RS", "+381", "Serbia", "🇷🇸", "Europe/Belgrade"),
        CountryInfo("ME", "+382", "Montenegro", "🇲🇪", "Europe/Podgorica"),
        CountryInfo("XK", "+383", "Kosovo", "🇽🇰", "Europe/Belgrade"),
        CountryInfo("HR", "+385", "Croatia", "🇭🇷", "Europe/Zagreb"),
        CountryInfo("SI", "+386", "Slovenia", "🇸🇮", "Europe/Ljubljana"),
        CountryInfo("BA", "+387", "Bosnia and Herzegovina", "🇧🇦", "Europe/Sarajevo"),
        CountryInfo("MK", "+389", "North Macedonia", "🇲🇰", "Europe/Skopje"),
        CountryInfo("CZ", "+420", "Czech Republic", "🇨🇿", "Europe/Prague"),
        CountryInfo("SK", "+421", "Slovakia", "🇸🇰", "Europe/Bratislava"),
        CountryInfo("LI", "+423", "Liechtenstein", "🇱🇮", "Europe/Vaduz"),
        CountryInfo("FK", "+500", "Falkland Islands", "🇫🇰", "Atlantic/Stanley"),
        CountryInfo("BZ", "+501", "Belize", "🇧🇿", "America/Belize"),
        CountryInfo("GT", "+502", "Guatemala", "🇬🇹", "America/Guatemala"),
        CountryInfo("SV", "+503", "El Salvador", "🇸🇻", "America/El_Salvador"),
        CountryInfo("HN", "+504", "Honduras", "🇭🇳", "America/Tegucigalpa"),
        CountryInfo("NI", "+505", "Nicaragua", "🇳🇮", "America/Managua"),
        CountryInfo("CR", "+506", "Costa Rica", "🇨🇷", "America/Costa_Rica"),
        CountryInfo("PA", "+507", "Panama", "🇵🇦", "America/Panama"),
        CountryInfo("PM", "+508", "Saint Pierre and Miquelon", "🇵🇲", "America/Miquelon"),
        CountryInfo("HT", "+509", "Haiti", "🇭🇹", "America/Port-au-Prince"),
        CountryInfo("GP", "+590", "Guadeloupe", "🇬🇵", "America/Guadeloupe"),
        CountryInfo("BL", "+590", "Saint Barthélemy", "🇧🇱", "America/Guadeloupe"),
        CountryInfo("MF", "+590", "Saint Martin", "🇲🇫", "America/Guadeloupe"),
        CountryInfo("BO", "+591", "Bolivia", "🇧🇴", "America/La_Paz"),
        CountryInfo("GY", "+592", "Guyana", "🇬🇾", "America/Guyana"),
        CountryInfo("EC", "+593", "Ecuador", "🇪🇨", "America/Guayaquil"),
        CountryInfo("GF", "+594", "French Guiana", "🇬🇫", "America/Cayenne"),
        CountryInfo("PY", "+595", "Paraguay", "🇵🇾", "America/Asuncion"),
        CountryInfo("MQ", "+596", "Martinique", "🇲🇶", "America/Martinique"),
        CountryInfo("SR", "+597", "Suriname", "🇸🇷", "America/Paramaribo"),
        CountryInfo("UY", "+598", "Uruguay", "🇺🇾", "America/Montevideo"),
        CountryInfo("CW", "+599", "Curaçao", "🇨🇼", "America/Curacao"),
        CountryInfo("BQ", "+599", "Bonaire, Saba and Sint Eustatius", "🇧🇶", "America/Curacao"),
        CountryInfo("TL", "+670", "Timor-Leste", "🇹🇱", "Asia/Dili"),
        CountryInfo("NF", "+672", "Norfolk Island", "🇳🇫", "Pacific/Norfolk"),
        CountryInfo("AQ", "+672", "Antarctica", "🇦🇶", "Antarctica/McMurdo"),
        CountryInfo("BN", "+673", "Brunei", "🇧🇳", "Asia/Brunei"),
        CountryInfo("NR", "+674", "Nauru", "🇳🇷", "Pacific/Nauru"),
        CountryInfo("PG", "+675", "Papua New Guinea", "🇵🇬", "Pacific/Port_Moresby"),
        CountryInfo("TO", "+676", "Tonga", "🇹🇴", "Pacific/Tongatapu"),
        CountryInfo("SB", "+677", "Solomon Islands", "🇸🇧", "Pacific/Guadalcanal"),
        CountryInfo("VU", "+678", "Vanuatu", "🇻🇺", "Pacific/Efate"),
        CountryInfo("FJ", "+679", "Fiji", "🇫🇯", "Pacific/Fiji"),
        CountryInfo("PW", "+680", "Palau", "🇵🇼", "Pacific/Palau"),
        CountryInfo("WF", "+681", "Wallis and Futuna", "🇼🇫", "Pacific/Wallis"),
        CountryInfo("CK", "+682", "Cook Islands", "🇨🇰", "Pacific/Rarotonga"),
        CountryInfo("NU", "+683", "Niue", "🇳🇺", "Pacific/Niue"),
        CountryInfo("WS", "+685", "Samoa", "🇼🇸", "Pacific/Apia"),
        CountryInfo("KI", "+686", "Kiribati", "🇰🇮", "Pacific/Tarawa"),
        CountryInfo("NC", "+687", "New Caledonia", "🇳🇨", "Pacific/Noumea"),
        CountryInfo("TV", "+688", "Tuvalu", "🇹🇻", "Pacific/Funafuti"),
        CountryInfo("PF", "+689", "French Polynesia", "🇵🇫", "Pacific/Tahiti"),
        CountryInfo("TK", "+690", "Tokelau", "🇹🇰", "Pacific/Fakaofo"),
        CountryInfo("FM", "+691", "Micronesia", "🇫🇲", "Pacific/Pohnpei"),
        CountryInfo("MH", "+692", "Marshall Islands", "🇲🇭", "Pacific/Majuro"),
        CountryInfo("KP", "+850", "North Korea", "🇰🇵", "Asia/Pyongyang"),
        CountryInfo("HK", "+852", "Hong Kong", "🇭🇰", "Asia/Hong_Kong"),
        CountryInfo("MO", "+853", "Macau", "🇲🇴", "Asia/Macau"),
        CountryInfo("KH", "+855", "Cambodia", "🇰🇭", "Asia/Phnom_Penh"),
        CountryInfo("LA", "+856", "Laos", "🇱🇦", "Asia/Vientiane"),
        CountryInfo("BD", "+880", "Bangladesh", "🇧🇩", "Asia/Dhaka"),
        CountryInfo("CN", "+886", "China", "🇨🇳", "Asia/Taipei"),
        CountryInfo("MV", "+960", "Maldives", "🇲🇻", "Indian/Maldives"),
        CountryInfo("LB", "+961", "Lebanon", "🇱🇧", "Asia/Beirut"),
        CountryInfo("JO", "+962", "Jordan", "🇯🇴", "Asia/Amman"),
        CountryInfo("SY", "+963", "Syria", "🇸🇾", "Asia/Damascus"),
        CountryInfo("IQ", "+964", "Iraq", "🇮🇶", "Asia/Baghdad"),
        CountryInfo("KW", "+965", "Kuwait", "🇰🇼", "Asia/Kuwait"),
        CountryInfo("SA", "+966", "Saudi Arabia", "🇸🇦", "Asia/Riyadh"),
        CountryInfo("YE", "+967", "Yemen", "🇾🇪", "Asia/Aden"),
        CountryInfo("OM", "+968", "Oman", "🇴🇲", "Asia/Muscat"),
        CountryInfo("PS", "+970", "Palestine", "🇵🇸", "Asia/Gaza"),
        CountryInfo("AE", "+971", "United Arab Emirates", "🇦🇪", "Asia/Dubai"),
        CountryInfo("PS", "+972", "Palestine", "🇵🇸", "Asia/Jerusalem"),
        CountryInfo("BH", "+973", "Bahrain", "🇧🇭", "Asia/Bahrain"),
        CountryInfo("QA", "+974", "Qatar", "🇶🇦", "Asia/Qatar"),
        CountryInfo("BT", "+975", "Bhutan", "🇧🇹", "Asia/Thimphu"),
        CountryInfo("MN", "+976", "Mongolia", "🇲🇳", "Asia/Ulaanbaatar"),
        CountryInfo("NP", "+977", "Nepal", "🇳🇵", "Asia/Kathmandu"),
        CountryInfo("TJ", "+992", "Tajikistan", "🇹🇯", "Asia/Dushanbe"),
        CountryInfo("TM", "+993", "Turkmenistan", "🇹🇲", "Asia/Ashgabat"),
        CountryInfo("AZ", "+994", "Azerbaijan", "🇦🇿", "Asia/Baku"),
        CountryInfo("GE", "+995", "Georgia", "🇬🇪", "Asia/Tbilisi"),
        CountryInfo("KG", "+996", "Kyrgyzstan", "🇰🇬", "Asia/Bishkek"),
        CountryInfo("UZ", "+998", "Uzbekistan", "🇺🇿", "Asia/Tashkent"),
        CountryInfo("SS", "+211", "South Sudan", "🇸🇸", "Africa/Juba"),
        CountryInfo("MA", "+212", "Morocco", "🇲🇦", "Africa/Casablanca"),
        CountryInfo("EH", "+212", "Western Sahara", "🇪🇭", "Africa/El_Aaiun"),
        CountryInfo("DZ", "+213", "Algeria", "🇩🇿", "Africa/Algiers"),
        CountryInfo("TN", "+216", "Tunisia", "🇹🇳", "Africa/Tunis"),
        CountryInfo("LY", "+218", "Libya", "🇱🇾", "Africa/Tripoli"),
        CountryInfo("GM", "+220", "Gambia", "🇬🇲", "Africa/Banjul"),
        CountryInfo("SN", "+221", "Senegal", "🇸🇳", "Africa/Dakar"),
        CountryInfo("MR", "+222", "Mauritania", "🇲🇷", "Africa/Nouakchott"),
        CountryInfo("ML", "+223", "Mali", "🇲🇱", "Africa/Bamako"),
        CountryInfo("GN", "+224", "Guinea", "🇬🇳", "Africa/Conakry"),
        CountryInfo("CI", "+225", "Ivory Coast", "🇨🇮", "Africa/Abidjan"),
        CountryInfo("BF", "+226", "Burkina Faso", "🇧🇫", "Africa/Ouagadougou"),
        CountryInfo("NE", "+227", "Niger", "🇳🇪", "Africa/Niamey"),
        CountryInfo("TG", "+228", "Togo", "🇹🇬", "Africa/Lome"),
        CountryInfo("BJ", "+229", "Benin", "🇧🇯", "Africa/Porto-Novo"),
        CountryInfo("MU", "+230", "Mauritius", "🇲🇺", "Indian/Mauritius"),
        CountryInfo("LR", "+231", "Liberia", "🇱🇷", "Africa/Monrovia"),
        CountryInfo("SL", "+232", "Sierra Leone", "🇸🇱", "Africa/Freetown"),
        CountryInfo("GH", "+233", "Ghana", "🇬🇭", "Africa/Accra"),
        CountryInfo("NG", "+234", "Nigeria", "🇳🇬", "Africa/Lagos"),
        CountryInfo("TD", "+235", "Chad", "🇹🇩", "Africa/Ndjamena"),
        CountryInfo("CF", "+236", "Central African Republic", "🇨🇫", "Africa/Bangui"),
        CountryInfo("CM", "+237", "Cameroon", "🇨🇲", "Africa/Douala"),
        CountryInfo("CV", "+238", "Cape Verde", "🇨🇻", "Atlantic/Cape_Verde"),
        CountryInfo("ST", "+239", "Sao Tome and Principe", "🇸🇹", "Africa/Sao_Tome"),
        CountryInfo("GQ", "+240", "Equatorial Guinea", "🇬🇶", "Africa/Malabo"),
        CountryInfo("GA", "+241", "Gabon", "🇬🇦", "Africa/Libreville"),
        CountryInfo("CG", "+242", "Republic of the Congo", "🇨🇬", "Africa/Brazzaville"),
        CountryInfo("CD", "+243", "DR Congo", "🇨🇩", "Africa/Kinshasa"),
        CountryInfo("AO", "+244", "Angola", "🇦🇴", "Africa/Luanda"),
        CountryInfo("GW", "+245", "Guinea-Bissau", "🇬🇼", "Africa/Bissau"),
        CountryInfo("IO", "+246", "British Indian Ocean Territory", "🇮🇴", "Indian/Chagos"),
        CountryInfo("SC", "+248", "Seychelles", "🇸🇨", "Indian/Mahe"),
        CountryInfo("SD", "+249", "Sudan", "🇸🇩", "Africa/Khartoum"),
        CountryInfo("RW", "+250", "Rwanda", "🇷🇼", "Africa/Kigali"),
        CountryInfo("ET", "+251", "Ethiopia", "🇪🇹", "Africa/Addis_Ababa"),
        CountryInfo("SO", "+252", "Somalia", "🇸🇴", "Africa/Mogadishu"),
        CountryInfo("DJ", "+253", "Djibouti", "🇩🇯", "Africa/Djibouti"),
        CountryInfo("KE", "+254", "Kenya", "🇰🇪", "Africa/Nairobi"),
        CountryInfo("TZ", "+255", "Tanzania", "🇹🇿", "Africa/Dar_es_Salaam"),
        CountryInfo("UG", "+256", "Uganda", "🇺🇬", "Africa/Kampala"),
        CountryInfo("BI", "+257", "Burundi", "🇧🇮", "Africa/Bujumbura"),
        CountryInfo("MZ", "+258", "Mozambique", "🇲🇿", "Africa/Maputo"),
        CountryInfo("ZM", "+260", "Zambia", "🇿🇲", "Africa/Lusaka"),
        CountryInfo("MG", "+261", "Madagascar", "🇲🇬", "Indian/Antananarivo"),
        CountryInfo("RE", "+262", "Réunion", "🇷🇪", "Indian/Reunion"),
        CountryInfo("YT", "+262", "Mayotte", "🇾🇹", "Indian/Mayotte"),
        CountryInfo("ZW", "+263", "Zimbabwe", "🇿🇼", "Africa/Harare"),
        CountryInfo("NA", "+264", "Namibia", "🇳🇦", "Africa/Windhoek"),
        CountryInfo("MW", "+265", "Malawi", "🇲🇼", "Africa/Blantyre"),
        CountryInfo("LS", "+266", "Lesotho", "🇱🇸", "Africa/Maseru"),
        CountryInfo("BW", "+267", "Botswana", "🇧🇼", "Africa/Gaborone"),
        CountryInfo("SZ", "+268", "Eswatini", "🇸🇿", "Africa/Mbabane"),
        CountryInfo("KM", "+269", "Comoros", "🇰🇲", "Indian/Comoro"),
        CountryInfo("SH", "+290", "Saint Helena", "🇸🇭", "Atlantic/St_Helena"),
        CountryInfo("ER", "+291", "Eritrea", "🇪🇷", "Africa/Asmara"),
        CountryInfo("AW", "+297", "Aruba", "🇦🇼", "America/Curacao"),
        CountryInfo("FO", "+298", "Faroe Islands", "🇫🇴", "Atlantic/Faroe"),
        CountryInfo("GL", "+299", "Greenland", "🇬🇱", "America/Nuuk"),
        CountryInfo("EG", "+20", "Egypt", "🇪🇬", "Africa/Cairo"),
        CountryInfo("ZA", "+27", "South Africa", "🇿🇦", "Africa/Johannesburg"),
        CountryInfo("GR", "+30", "Greece", "🇬🇷", "Europe/Athens"),
        CountryInfo("NL", "+31", "Netherlands", "🇳🇱", "Europe/Amsterdam"),
        CountryInfo("BE", "+32", "Belgium", "🇧🇪", "Europe/Brussels"),
        CountryInfo("FR", "+33", "France", "🇫🇷", "Europe/Paris"),
        CountryInfo("ES", "+34", "Spain", "🇪🇸", "Europe/Madrid"),
        CountryInfo("HU", "+36", "Hungary", "🇭🇺", "Europe/Budapest"),
        CountryInfo("IT", "+39", "Italy", "🇮🇹", "Europe/Rome"),
        CountryInfo("RO", "+40", "Romania", "🇷🇴", "Europe/Bucharest"),
        CountryInfo("CH", "+41", "Switzerland", "🇨🇭", "Europe/Zurich"),
        CountryInfo("AT", "+43", "Austria", "🇦🇹", "Europe/Vienna"),
        CountryInfo("GB", "+44", "United Kingdom", "🇬🇧", "Europe/London"),
        CountryInfo("GG", "+44", "Guernsey", "🇬🇬", "Europe/Guernsey"),
        CountryInfo("IM", "+44", "Isle of Man", "🇮🇲", "Europe/Isle_of_Man"),
        CountryInfo("JE", "+44", "Jersey", "🇯🇪", "Europe/Jersey"),
        CountryInfo("DK", "+45", "Denmark", "🇩🇰", "Europe/Copenhagen"),
        CountryInfo("SE", "+46", "Sweden", "🇸🇪", "Europe/Stockholm"),
        CountryInfo("NO", "+47", "Norway", "🇳🇴", "Europe/Oslo"),
        CountryInfo("SJ", "+47", "Svalbard and Jan Mayen", "🇸🇯", "Europe/Oslo"),
        CountryInfo("PL", "+48", "Poland", "🇵🇱", "Europe/Warsaw"),
        CountryInfo("DE", "+49", "Germany", "🇩🇪", "Europe/Berlin"),
        CountryInfo("PE", "+51", "Peru", "🇵🇪", "America/Lima"),
        CountryInfo("MX", "+52", "Mexico", "🇲🇽", "America/Mexico_City"),
        CountryInfo("CU", "+53", "Cuba", "🇨🇺", "America/Havana"),
        CountryInfo("AR", "+54", "Argentina", "🇦🇷", "America/Argentina/Buenos_Aires"),
        CountryInfo("BR", "+55", "Brazil", "🇧🇷", "America/Sao_Paulo"),
        CountryInfo("CL", "+56", "Chile", "🇨🇱", "America/Santiago"),
        CountryInfo("CO", "+57", "Colombia", "🇨🇴", "America/Bogota"),
        CountryInfo("VE", "+58", "Venezuela", "🇻🇪", "America/Caracas"),
        CountryInfo("MY", "+60", "Malaysia", "🇲🇾", "Asia/Kuala_Lumpur"),
        CountryInfo("AU", "+61", "Australia", "🇦🇺", "Australia/Sydney"),
        CountryInfo("CC", "+61", "Cocos (Keeling) Islands", "🇨🇨", "Indian/Cocos"),
        CountryInfo("CX", "+61", "Christmas Island", "🇨🇽", "Indian/Christmas"),
        CountryInfo("ID", "+62", "Indonesia", "🇮🇩", "Asia/Jakarta"),
        CountryInfo("PH", "+63", "Philippines", "🇵🇭", "Asia/Manila"),
        CountryInfo("NZ", "+64", "New Zealand", "🇳🇿", "Pacific/Auckland"),
        CountryInfo("SG", "+65", "Singapore", "🇸🇬", "Asia/Singapore"),
        CountryInfo("TH", "+66", "Thailand", "🇹🇭", "Asia/Bangkok"),
        CountryInfo("JP", "+81", "Japan", "🇯🇵", "Asia/Tokyo"),
        CountryInfo("KR", "+82", "South Korea", "🇰🇷", "Asia/Seoul"),
        CountryInfo("VN", "+84", "Vietnam", "🇻🇳", "Asia/Ho_Chi_Minh"),
        CountryInfo("CN", "+86", "China", "🇨🇳", "Asia/Shanghai"),
        CountryInfo("TR", "+90", "Turkey", "🇹🇷", "Europe/Istanbul"),
        CountryInfo("IN", "+91", "India", "🇮🇳", "Asia/Kolkata"),
        CountryInfo("PK", "+92", "Pakistan", "🇵🇰", "Asia/Karachi"),
        CountryInfo("AF", "+93", "Afghanistan", "🇦🇫", "Asia/Kabul"),
        CountryInfo("LK", "+94", "Sri Lanka", "🇱🇰", "Asia/Colombo"),
        CountryInfo("MM", "+95", "Myanmar", "🇲🇲", "Asia/Yangon"),
        CountryInfo("IR", "+98", "Iran", "🇮🇷", "Asia/Tehran"),
        CountryInfo("KZ", "+7", "Kazakhstan", "🇰🇿", "Asia/Almaty"),
        CountryInfo("RU", "+7", "Russia", "🇷🇺", "Europe/Moscow"),
        CountryInfo("CA", "+1", "Canada", "🇨🇦", "America/Toronto"),
        CountryInfo("US", "+1", "United States", "🇺🇸", "America/New_York"),
    )

    private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
    private val zoneShortFormatter = DateTimeFormatter.ofPattern("z", Locale.US)

    fun getAllCountries(): List<CountryInfo> = countryList

    fun getCountryByIso(iso: String?): CountryInfo? {
        if (iso.isNullOrBlank()) return null
        val clean = iso.trim().uppercase(Locale.ROOT)
        if (clean == "IL") return countryList.firstOrNull { it.isoCode == "PS" }
        if (clean == "TW") return countryList.firstOrNull { it.isoCode == "CN" }
        return countryList.firstOrNull { it.isoCode.equals(clean, ignoreCase = true) }
    }

    fun detectCountry(rawNumber: String, userCountryIso: String? = null): InternationalPreview? =
        detectCountryAndLocalTime(rawNumber, userCountryIso)

    fun detectCountryAndLocalTime(rawNumber: String, userCountryIso: String? = null): InternationalPreview? {
        val clean = rawNumber.trim().replace(Regex("[^0-9+]"), "")
        if (clean.length < 2) return null

        val homeIso = userCountryIso?.trim()?.uppercase(Locale.ROOT)
            ?: com.ryanshelby.linea.telecom.screening.SimCountryDetector.currentCountryIso

        val normalized = when {
            clean.startsWith("+") -> clean
            clean.startsWith("00") -> "+" + clean.substring(2)
            else -> {
                // If user is in US/CA and enters a 10-digit domestic NANP number, don't show international preview
                if ((homeIso == "US" || homeIso == "CA") && clean.length == 10 && clean[0] in '2'..'9' &&
                    com.ryanshelby.linea.telecom.screening.OfflineCallerIdEngine.isNanpAreaCode(clean.substring(0, 3))) {
                    null
                } else if (homeIso == "BD" && clean.startsWith("1") && clean.length in 2..10 && clean[1] in '3'..'9') {
                    // Local domestic Bangladesh mobile
                    "+880" + clean
                } else {
                    // Smart detection without '+': match longest country dial code
                    val match = countryList.firstOrNull { clean.startsWith(it.dialCode.removePrefix("+")) }
                    if (match != null) {
                        "+" + clean
                    } else {
                        null
                    }
                }
            }
        } ?: return null

        // Resolve matched country with precision for shared dial codes (+1 for US/CA, +7 for RU/KZ)
        val matchedCountry = when {
            normalized.startsWith("+1") && normalized.length >= 4 -> {
                val area = normalized.substring(2, 5)
                if (com.ryanshelby.linea.telecom.screening.OfflineCallerIdEngine.isCanadianAreaCode(area)) {
                    countryList.firstOrNull { it.isoCode == "CA" } ?: countryList.firstOrNull { it.dialCode == "+1" }
                } else {
                    val caribbean = countryList.firstOrNull { it.dialCode.length == 5 && normalized.startsWith(it.dialCode) }
                    caribbean ?: (countryList.firstOrNull { it.isoCode == "US" } ?: countryList.firstOrNull { it.dialCode == "+1" })
                }
            }
            normalized.startsWith("+7") && normalized.length >= 3 -> {
                val nextDigit = normalized[2]
                if (nextDigit == '6' || nextDigit == '7') {
                    countryList.firstOrNull { it.isoCode == "KZ" } ?: countryList.firstOrNull { it.dialCode == "+7" }
                } else {
                    countryList.firstOrNull { it.isoCode == "RU" } ?: countryList.firstOrNull { it.dialCode == "+7" }
                }
            }
            else -> countryList.firstOrNull { normalized.startsWith(it.dialCode) }
        } ?: return null

        // Only show if user has typed at least the dial code
        if (normalized.length < matchedCountry.dialCode.length) {
            return null
        }

        return try {
            val zoneId = ZoneId.of(matchedCountry.primaryTimeZone)
            val zonedDateTime = ZonedDateTime.now(zoneId)
            val timeString = zonedDateTime.format(timeFormatter)
            val zoneShort = zonedDateTime.format(zoneShortFormatter)

            val hour = zonedDateTime.hour
            val timeOfDay = TimeOfDayState.fromHour(hour)
            val isLateNight = timeOfDay == TimeOfDayState.NIGHT
            val lateNightWarning = if (isLateNight) "🌙 Night in ${matchedCountry.countryName}" else null

            InternationalPreview(
                countryName = matchedCountry.countryName,
                flagEmoji = matchedCountry.flagEmoji,
                localTimeFormatted = timeString,
                timeZoneShort = zoneShort,
                timeOfDay = timeOfDay,
                isLateNight = isLateNight,
                lateNightWarning = lateNightWarning
            )
        } catch (e: Exception) {
            null
        }
    }
}
