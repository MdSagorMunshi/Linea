package com.ryanshelby.linea.telecom.screening

data class CallerIdResult(
    val category: String,          // Emergency, Toll-Free, Regional Location, Mobile Carrier, International, Standard
    val regionOrCountry: String,   // e.g. "San Francisco, CA • United States", "Grameenphone • Bangladesh"
    val isEmergency: Boolean = false,
    val isTollFree: Boolean = false,
    val badgeLabel: String,        // e.g. "SAN FRANCISCO, CA", "TOLL-FREE", "GRAMEENPHONE"
    val flagEmoji: String = "🌐",
    val cityOrState: String? = null,
    val carrierOrType: String? = null,
    val countryName: String? = null,
    val isSimCountry: Boolean = false
)

object OfflineCallerIdEngine {

    private val EMERGENCY_NUMBERS = setOf(
        "911", "112", "999", "000", "100", "101", "102", "110", "119", "199", "993", "912"
    )

    private val TOLL_FREE_PREFIXES = listOf(
        "+1800", "1800", "800",
        "+1888", "1888", "888",
        "+1877", "1877", "877",
        "+1866", "1866", "866",
        "+1855", "1855", "855",
        "+1844", "1844", "844",
        "+1833", "1833", "833",
        "0800", "0808", "+44800", "+44808"
    )

    data class CountryInfo(
        val dialCode: String,
        val iso: String,
        val name: String,
        val flag: String
    )

    // Complete 195+ sovereign countries worldwide (sorted by dialCode length descending)
    private val ALL_COUNTRIES = listOf(
        // 4-digit NANP territories & specific islands
        CountryInfo("+1242", "BS", "Bahamas", "🇧🇸"),
        CountryInfo("+1246", "BB", "Barbados", "🇧🇧"),
        CountryInfo("+1264", "AI", "Anguilla", "🇦🇮"),
        CountryInfo("+1268", "AG", "Antigua and Barbuda", "🇦🇬"),
        CountryInfo("+1284", "VG", "British Virgin Islands", "🇻🇬"),
        CountryInfo("+1340", "VI", "U.S. Virgin Islands", "🇻🇮"),
        CountryInfo("+1345", "KY", "Cayman Islands", "🇰🇾"),
        CountryInfo("+1441", "BM", "Bermuda", "🇧🇲"),
        CountryInfo("+1473", "GD", "Grenada", "🇬🇩"),
        CountryInfo("+1649", "TC", "Turks and Caicos Islands", "🇹🇨"),
        CountryInfo("+1658", "JM", "Jamaica", "🇯🇲"),
        CountryInfo("+1664", "MS", "Montserrat", "🇲🇸"),
        CountryInfo("+1670", "MP", "Northern Mariana Islands", "🇲🇵"),
        CountryInfo("+1671", "GU", "Guam", "🇬🇺"),
        CountryInfo("+1684", "AS", "American Samoa", "🇦🇸"),
        CountryInfo("+1721", "SX", "Sint Maarten", "🇸🇽"),
        CountryInfo("+1758", "LC", "Saint Lucia", "🇱🇨"),
        CountryInfo("+1767", "DM", "Dominica", "🇩🇲"),
        CountryInfo("+1784", "VC", "Saint Vincent and the Grenadines", "🇻🇨"),
        CountryInfo("+1787", "PR", "Puerto Rico", "🇵🇷"),
        CountryInfo("+1809", "DO", "Dominican Republic", "🇩🇴"),
        CountryInfo("+1829", "DO", "Dominican Republic", "🇩🇴"),
        CountryInfo("+1849", "DO", "Dominican Republic", "🇩🇴"),
        CountryInfo("+1868", "TT", "Trinidad and Tobago", "🇹🇹"),
        CountryInfo("+1869", "KN", "Saint Kitts and Nevis", "🇰🇳"),
        CountryInfo("+1876", "JM", "Jamaica", "🇯🇲"),
        CountryInfo("+1939", "PR", "Puerto Rico", "🇵🇷"),

        // 3-digit prefixes
        CountryInfo("+350", "GI", "Gibraltar", "🇬🇮"),
        CountryInfo("+351", "PT", "Portugal", "🇵🇹"),
        CountryInfo("+352", "LU", "Luxembourg", "🇱🇺"),
        CountryInfo("+353", "IE", "Ireland", "🇮🇪"),
        CountryInfo("+354", "IS", "Iceland", "🇮🇸"),
        CountryInfo("+355", "AL", "Albania", "🇦🇱"),
        CountryInfo("+356", "MT", "Malta", "🇲🇹"),
        CountryInfo("+357", "CY", "Cyprus", "🇨🇾"),
        CountryInfo("+358", "FI", "Finland", "🇫🇮"),
        CountryInfo("+359", "BG", "Bulgaria", "🇧🇬"),
        CountryInfo("+370", "LT", "Lithuania", "🇱🇹"),
        CountryInfo("+371", "LV", "Latvia", "🇱🇻"),
        CountryInfo("+372", "EE", "Estonia", "🇪🇪"),
        CountryInfo("+373", "MD", "Moldova", "🇲🇩"),
        CountryInfo("+374", "AM", "Armenia", "🇦🇲"),
        CountryInfo("+375", "BY", "Belarus", "🇧🇾"),
        CountryInfo("+376", "AD", "Andorra", "🇦🇩"),
        CountryInfo("+377", "MC", "Monaco", "🇲🇨"),
        CountryInfo("+378", "SM", "San Marino", "🇸🇲"),
        CountryInfo("+379", "VA", "Vatican City", "🇻🇦"),
        CountryInfo("+380", "UA", "Ukraine", "🇺🇦"),
        CountryInfo("+381", "RS", "Serbia", "🇷🇸"),
        CountryInfo("+382", "ME", "Montenegro", "🇲🇪"),
        CountryInfo("+383", "XK", "Kosovo", "🇽🇰"),
        CountryInfo("+385", "HR", "Croatia", "🇭🇷"),
        CountryInfo("+386", "SI", "Slovenia", "🇸🇮"),
        CountryInfo("+387", "BA", "Bosnia and Herzegovina", "🇧🇦"),
        CountryInfo("+389", "MK", "North Macedonia", "🇲🇰"),
        CountryInfo("+420", "CZ", "Czech Republic", "🇨🇿"),
        CountryInfo("+421", "SK", "Slovakia", "🇸🇰"),
        CountryInfo("+423", "LI", "Liechtenstein", "🇱🇮"),
        CountryInfo("+500", "FK", "Falkland Islands", "🇫🇰"),
        CountryInfo("+501", "BZ", "Belize", "🇧🇿"),
        CountryInfo("+502", "GT", "Guatemala", "🇬🇹"),
        CountryInfo("+503", "SV", "El Salvador", "🇸🇻"),
        CountryInfo("+504", "HN", "Honduras", "🇭🇳"),
        CountryInfo("+505", "NI", "Nicaragua", "🇳🇮"),
        CountryInfo("+506", "CR", "Costa Rica", "🇨🇷"),
        CountryInfo("+507", "PA", "Panama", "🇵🇦"),
        CountryInfo("+508", "PM", "Saint Pierre and Miquelon", "🇵🇲"),
        CountryInfo("+509", "HT", "Haiti", "🇭🇹"),
        CountryInfo("+590", "GP", "Guadeloupe", "🇬🇵"),
        CountryInfo("+591", "BO", "Bolivia", "🇧🇴"),
        CountryInfo("+592", "GY", "Guyana", "🇬🇾"),
        CountryInfo("+593", "EC", "Ecuador", "🇪🇨"),
        CountryInfo("+594", "GF", "French Guiana", "🇬🇫"),
        CountryInfo("+595", "PY", "Paraguay", "🇵🇾"),
        CountryInfo("+596", "MQ", "Martinique", "🇲🇶"),
        CountryInfo("+597", "SR", "Suriname", "🇸🇷"),
        CountryInfo("+598", "UY", "Uruguay", "🇺🇾"),
        CountryInfo("+599", "CW", "Curaçao", "🇨🇼"),
        CountryInfo("+670", "TL", "Timor-Leste", "🇹🇱"),
        CountryInfo("+672", "NF", "Norfolk Island", "🇳🇫"),
        CountryInfo("+673", "BN", "Brunei", "🇧🇳"),
        CountryInfo("+674", "NR", "Nauru", "🇳🇷"),
        CountryInfo("+675", "PG", "Papua New Guinea", "🇵🇬"),
        CountryInfo("+676", "TO", "Tonga", "🇹🇴"),
        CountryInfo("+677", "SB", "Solomon Islands", "🇸🇧"),
        CountryInfo("+678", "VU", "Vanuatu", "🇻🇺"),
        CountryInfo("+679", "FJ", "Fiji", "🇫🇯"),
        CountryInfo("+680", "PW", "Palau", "🇵🇼"),
        CountryInfo("+681", "WF", "Wallis and Futuna", "🇼🇫"),
        CountryInfo("+682", "CK", "Cook Islands", "🇨🇰"),
        CountryInfo("+683", "NU", "Niue", "🇳🇺"),
        CountryInfo("+685", "WS", "Samoa", "🇼🇸"),
        CountryInfo("+686", "KI", "Kiribati", "🇰🇮"),
        CountryInfo("+687", "NC", "New Caledonia", "🇳🇨"),
        CountryInfo("+688", "TV", "Tuvalu", "🇹🇻"),
        CountryInfo("+689", "PF", "French Polynesia", "🇵🇫"),
        CountryInfo("+690", "TK", "Tokelau", "🇹🇰"),
        CountryInfo("+691", "FM", "Micronesia", "🇫🇲"),
        CountryInfo("+692", "MH", "Marshall Islands", "🇲🇭"),
        CountryInfo("+850", "KP", "North Korea", "🇰🇵"),
        CountryInfo("+852", "HK", "Hong Kong", "🇭🇰"),
        CountryInfo("+853", "MO", "Macau", "🇲🇴"),
        CountryInfo("+855", "KH", "Cambodia", "🇰🇭"),
        CountryInfo("+856", "LA", "Laos", "🇱🇦"),
        CountryInfo("+880", "BD", "Bangladesh", "🇧🇩"),
        CountryInfo("+886", "TW", "Taiwan", "🇹🇼"),
        CountryInfo("+960", "MV", "Maldives", "🇲🇻"),
        CountryInfo("+961", "LB", "Lebanon", "🇱🇧"),
        CountryInfo("+962", "JO", "Jordan", "🇯🇴"),
        CountryInfo("+963", "SY", "Syria", "🇸🇾"),
        CountryInfo("+964", "IQ", "Iraq", "🇮🇶"),
        CountryInfo("+965", "KW", "Kuwait", "🇰🇼"),
        CountryInfo("+966", "SA", "Saudi Arabia", "🇸🇦"),
        CountryInfo("+967", "YE", "Yemen", "🇾🇪"),
        CountryInfo("+968", "OM", "Oman", "🇴🇲"),
        CountryInfo("+970", "PS", "Palestine", "🇵🇸"),
        CountryInfo("+971", "AE", "United Arab Emirates", "🇦🇪"),
        CountryInfo("+972", "IL", "Israel", "🇮🇱"),
        CountryInfo("+973", "BH", "Bahrain", "🇧🇭"),
        CountryInfo("+974", "QA", "Qatar", "🇶🇦"),
        CountryInfo("+975", "BT", "Bhutan", "🇧🇹"),
        CountryInfo("+976", "MN", "Mongolia", "🇲🇳"),
        CountryInfo("+977", "NP", "Nepal", "🇳🇵"),
        CountryInfo("+992", "TJ", "Tajikistan", "🇹🇯"),
        CountryInfo("+993", "TM", "Turkmenistan", "🇹🇲"),
        CountryInfo("+994", "AZ", "Azerbaijan", "🇦🇿"),
        CountryInfo("+995", "GE", "Georgia", "🇬🇪"),
        CountryInfo("+996", "KG", "Kyrgyzstan", "🇰🇬"),
        CountryInfo("+998", "UZ", "Uzbekistan", "🇺🇿"),

        // Africa 3-digit
        CountryInfo("+211", "SS", "South Sudan", "🇸🇸"),
        CountryInfo("+212", "MA", "Morocco", "🇲🇦"),
        CountryInfo("+213", "DZ", "Algeria", "🇩🇿"),
        CountryInfo("+216", "TN", "Tunisia", "🇹🇳"),
        CountryInfo("+218", "LY", "Libya", "🇱🇾"),
        CountryInfo("+220", "GM", "Gambia", "🇬🇲"),
        CountryInfo("+221", "SN", "Senegal", "🇸🇳"),
        CountryInfo("+222", "MR", "Mauritania", "🇲🇷"),
        CountryInfo("+223", "ML", "Mali", "🇲🇱"),
        CountryInfo("+224", "GN", "Guinea", "🇬🇳"),
        CountryInfo("+225", "CI", "Ivory Coast", "🇨🇮"),
        CountryInfo("+226", "BF", "Burkina Faso", "🇧🇫"),
        CountryInfo("+227", "NE", "Niger", "🇳🇪"),
        CountryInfo("+228", "TG", "Togo", "🇹🇬"),
        CountryInfo("+229", "BJ", "Benin", "🇧🇯"),
        CountryInfo("+230", "MU", "Mauritius", "🇲🇺"),
        CountryInfo("+231", "LR", "Liberia", "🇱🇷"),
        CountryInfo("+232", "SL", "Sierra Leone", "🇸🇱"),
        CountryInfo("+233", "GH", "Ghana", "🇬🇭"),
        CountryInfo("+234", "NG", "Nigeria", "🇳🇬"),
        CountryInfo("+235", "TD", "Chad", "🇹🇩"),
        CountryInfo("+236", "CF", "Central African Republic", "🇨🇫"),
        CountryInfo("+237", "CM", "Cameroon", "🇨🇲"),
        CountryInfo("+238", "CV", "Cape Verde", "🇨🇻"),
        CountryInfo("+239", "ST", "Sao Tome and Principe", "🇸🇹"),
        CountryInfo("+240", "GQ", "Equatorial Guinea", "🇬🇶"),
        CountryInfo("+241", "GA", "Gabon", "🇬🇦"),
        CountryInfo("+242", "CG", "Republic of the Congo", "🇨🇬"),
        CountryInfo("+243", "CD", "DR Congo", "🇨🇩"),
        CountryInfo("+244", "AO", "Angola", "🇦🇴"),
        CountryInfo("+245", "GW", "Guinea-Bissau", "🇬🇼"),
        CountryInfo("+248", "SC", "Seychelles", "🇸🇨"),
        CountryInfo("+249", "SD", "Sudan", "🇸🇩"),
        CountryInfo("+250", "RW", "Rwanda", "🇷🇼"),
        CountryInfo("+251", "ET", "Ethiopia", "🇪🇹"),
        CountryInfo("+252", "SO", "Somalia", "🇸🇴"),
        CountryInfo("+253", "DJ", "Djibouti", "🇩🇯"),
        CountryInfo("+254", "KE", "Kenya", "🇰🇪"),
        CountryInfo("+255", "TZ", "Tanzania", "🇹🇿"),
        CountryInfo("+256", "UG", "Uganda", "🇺🇬"),
        CountryInfo("+257", "BI", "Burundi", "🇧🇮"),
        CountryInfo("+258", "MZ", "Mozambique", "🇲🇿"),
        CountryInfo("+260", "ZM", "Zambia", "🇿🇲"),
        CountryInfo("+261", "MG", "Madagascar", "🇲🇬"),
        CountryInfo("+262", "RE", "Réunion", "🇷🇪"),
        CountryInfo("+263", "ZW", "Zimbabwe", "🇿🇼"),
        CountryInfo("+264", "NA", "Namibia", "🇳🇦"),
        CountryInfo("+265", "MW", "Malawi", "🇲🇼"),
        CountryInfo("+266", "LS", "Lesotho", "🇱🇸"),
        CountryInfo("+267", "BW", "Botswana", "🇧🇼"),
        CountryInfo("+268", "SZ", "Eswatini", "🇸🇿"),
        CountryInfo("+269", "KM", "Comoros", "🇰🇲"),
        CountryInfo("+290", "SH", "Saint Helena", "🇸🇭"),
        CountryInfo("+291", "ER", "Eritrea", "🇪🇷"),
        CountryInfo("+297", "AW", "Aruba", "🇦🇼"),
        CountryInfo("+298", "FO", "Faroe Islands", "🇫🇴"),
        CountryInfo("+299", "GL", "Greenland", "🇬🇱"),

        // 2-digit prefixes
        CountryInfo("+20", "EG", "Egypt", "🇪🇬"),
        CountryInfo("+27", "ZA", "South Africa", "🇿🇦"),
        CountryInfo("+30", "GR", "Greece", "🇬🇷"),
        CountryInfo("+31", "NL", "Netherlands", "🇳🇱"),
        CountryInfo("+32", "BE", "Belgium", "🇧🇪"),
        CountryInfo("+33", "FR", "France", "🇫🇷"),
        CountryInfo("+34", "ES", "Spain", "🇪🇸"),
        CountryInfo("+36", "HU", "Hungary", "🇭🇺"),
        CountryInfo("+39", "IT", "Italy", "🇮🇹"),
        CountryInfo("+40", "RO", "Romania", "🇷🇴"),
        CountryInfo("+41", "CH", "Switzerland", "🇨🇭"),
        CountryInfo("+43", "AT", "Austria", "🇦🇹"),
        CountryInfo("+44", "GB", "United Kingdom", "🇬🇧"),
        CountryInfo("+45", "DK", "Denmark", "🇩🇰"),
        CountryInfo("+46", "SE", "Sweden", "🇸🇪"),
        CountryInfo("+47", "NO", "Norway", "🇳🇴"),
        CountryInfo("+48", "PL", "Poland", "🇵🇱"),
        CountryInfo("+49", "DE", "Germany", "🇩🇪"),
        CountryInfo("+51", "PE", "Peru", "🇵🇪"),
        CountryInfo("+52", "MX", "Mexico", "🇲🇽"),
        CountryInfo("+53", "CU", "Cuba", "🇨🇺"),
        CountryInfo("+54", "AR", "Argentina", "🇦🇷"),
        CountryInfo("+55", "BR", "Brazil", "🇧🇷"),
        CountryInfo("+56", "CL", "Chile", "🇨🇱"),
        CountryInfo("+57", "CO", "Colombia", "🇨🇴"),
        CountryInfo("+58", "VE", "Venezuela", "🇻🇪"),
        CountryInfo("+60", "MY", "Malaysia", "🇲🇾"),
        CountryInfo("+61", "AU", "Australia", "🇦🇺"),
        CountryInfo("+62", "ID", "Indonesia", "🇮🇩"),
        CountryInfo("+63", "PH", "Philippines", "🇵🇭"),
        CountryInfo("+64", "NZ", "New Zealand", "🇳🇿"),
        CountryInfo("+65", "SG", "Singapore", "🇸🇬"),
        CountryInfo("+66", "TH", "Thailand", "🇹🇭"),
        CountryInfo("+81", "JP", "Japan", "🇯🇵"),
        CountryInfo("+82", "KR", "South Korea", "🇰🇷"),
        CountryInfo("+84", "VN", "Vietnam", "🇻🇳"),
        CountryInfo("+86", "CN", "China", "🇨🇳"),
        CountryInfo("+90", "TR", "Turkey", "🇹🇷"),
        CountryInfo("+91", "IN", "India", "🇮🇳"),
        CountryInfo("+92", "PK", "Pakistan", "🇵🇰"),
        CountryInfo("+93", "AF", "Afghanistan", "🇦🇫"),
        CountryInfo("+94", "LK", "Sri Lanka", "🇱🇰"),
        CountryInfo("+95", "MM", "Myanmar", "🇲🇲"),
        CountryInfo("+98", "IR", "Iran", "🇮🇷"),

        // 1-digit prefixes
        CountryInfo("+7", "RU", "Russia / Kazakhstan", "🇷🇺"),
        CountryInfo("+1", "US", "United States / Canada", "🇺🇸")
    ).sortedByDescending { it.dialCode.length }

    // NANP (US + Canada) Area Codes mapping to State/Province and Metro
    private val NANP_AREA_CODES = mapOf(
        // California
        "209" to ("Stockton / Modesto, CA" to "🇺🇸"),
        "213" to ("Downtown Los Angeles, CA" to "🇺🇸"),
        "310" to ("Beverly Hills / West LA, CA" to "🇺🇸"),
        "323" to ("Los Angeles, CA" to "🇺🇸"),
        "408" to ("San Jose / Silicon Valley, CA" to "🇺🇸"),
        "415" to ("San Francisco, CA" to "🇺🇸"),
        "510" to ("Oakland / East Bay, CA" to "🇺🇸"),
        "530" to ("Redding / Chico, CA" to "🇺🇸"),
        "559" to ("Fresno, CA" to "🇺🇸"),
        "562" to ("Long Beach, CA" to "🇺🇸"),
        "619" to ("San Diego, CA" to "🇺🇸"),
        "626" to ("Pasadena, CA" to "🇺🇸"),
        "628" to ("San Francisco, CA" to "🇺🇸"),
        "650" to ("San Mateo / Palo Alto, CA" to "🇺🇸"),
        "661" to ("Bakersfield, CA" to "🇺🇸"),
        "707" to ("Santa Rosa / Napa, CA" to "🇺🇸"),
        "714" to ("Anaheim / Orange County, CA" to "🇺🇸"),
        "760" to ("Palm Springs, CA" to "🇺🇸"),
        "805" to ("Santa Barbara / Ventura, CA" to "🇺🇸"),
        "818" to ("San Fernando Valley, CA" to "🇺🇸"),
        "858" to ("La Jolla / San Diego, CA" to "🇺🇸"),
        "909" to ("San Bernardino, CA" to "🇺🇸"),
        "916" to ("Sacramento, CA" to "🇺🇸"),
        "925" to ("Concord / Walnut Creek, CA" to "🇺🇸"),
        "949" to ("Irvine / Newport Beach, CA" to "🇺🇸"),
        "951" to ("Riverside, CA" to "🇺🇸"),

        // New York
        "212" to ("Manhattan, New York, NY" to "🇺🇸"),
        "315" to ("Syracuse, NY" to "🇺🇸"),
        "347" to ("New York City, NY" to "🇺🇸"),
        "516" to ("Hempstead / Long Island, NY" to "🇺🇸"),
        "518" to ("Albany, NY" to "🇺🇸"),
        "585" to ("Rochester, NY" to "🇺🇸"),
        "607" to ("Binghamton / Ithaca, NY" to "🇺🇸"),
        "631" to ("Suffolk County / Long Island, NY" to "🇺🇸"),
        "646" to ("Manhattan, New York, NY" to "🇺🇸"),
        "716" to ("Buffalo, NY" to "🇺🇸"),
        "718" to ("Brooklyn / Queens, NY" to "🇺🇸"),
        "845" to ("Poughkeepsie / Hudson Valley, NY" to "🇺🇸"),
        "914" to ("Westchester County, NY" to "🇺🇸"),
        "917" to ("New York City, NY" to "🇺🇸"),
        "929" to ("New York City, NY" to "🇺🇸"),

        // Texas
        "210" to ("San Antonio, TX" to "🇺🇸"),
        "214" to ("Dallas, TX" to "🇺🇸"),
        "254" to ("Waco, TX" to "🇺🇸"),
        "281" to ("Houston, TX" to "🇺🇸"),
        "325" to ("Abilene, TX" to "🇺🇸"),
        "361" to ("Corpus Christi, TX" to "🇺🇸"),
        "409" to ("Beaumont / Galveston, TX" to "🇺🇸"),
        "430" to ("Tyler, TX" to "🇺🇸"),
        "432" to ("Midland / Odessa, TX" to "🇺🇸"),
        "469" to ("Dallas, TX" to "🇺🇸"),
        "512" to ("Austin, TX" to "🇺🇸"),
        "713" to ("Houston, TX" to "🇺🇸"),
        "737" to ("Austin, TX" to "🇺🇸"),
        "806" to ("Lubbock / Amarillo, TX" to "🇺🇸"),
        "817" to ("Fort Worth, TX" to "🇺🇸"),
        "830" to ("New Braunfels, TX" to "🇺🇸"),
        "832" to ("Houston, TX" to "🇺🇸"),
        "903" to ("Tyler / Longview, TX" to "🇺🇸"),
        "915" to ("El Paso, TX" to "🇺🇸"),
        "936" to ("Conroe / Huntsville, TX" to "🇺🇸"),
        "940" to ("Denton / Wichita Falls, TX" to "🇺🇸"),
        "956" to ("Laredo / McAllen, TX" to "🇺🇸"),
        "972" to ("Dallas Metro, TX" to "🇺🇸"),
        "979" to ("Bryan / College Station, TX" to "🇺🇸"),

        // Illinois
        "312" to ("Chicago (Downtown), IL" to "🇺🇸"),
        "773" to ("Chicago, IL" to "🇺🇸"),
        "872" to ("Chicago, IL" to "🇺🇸"),
        "630" to ("Naperville / Aurora, IL" to "🇺🇸"),
        "847" to ("Evanston / Waukegan, IL" to "🇺🇸"),
        "217" to ("Springfield / Champaign, IL" to "🇺🇸"),
        "309" to ("Peoria / Bloomington, IL" to "🇺🇸"),
        "618" to ("Carbondale / Belleville, IL" to "🇺🇸"),
        "815" to ("Rockford / Joliet, IL" to "🇺🇸"),

        // Washington & Oregon
        "206" to ("Seattle, WA" to "🇺🇸"),
        "425" to ("Bellevue / Everett, WA" to "🇺🇸"),
        "253" to ("Tacoma, WA" to "🇺🇸"),
        "509" to ("Spokane / Yakima, WA" to "🇺🇸"),
        "360" to ("Olympia / Vancouver, WA" to "🇺🇸"),
        "503" to ("Portland / Salem, OR" to "🇺🇸"),
        "971" to ("Portland, OR" to "🇺🇸"),
        "541" to ("Eugene / Bend, OR" to "🇺🇸"),

        // Florida
        "305" to ("Miami, FL" to "🇺🇸"),
        "786" to ("Miami, FL" to "🇺🇸"),
        "407" to ("Orlando, FL" to "🇺🇸"),
        "813" to ("Tampa, FL" to "🇺🇸"),
        "904" to ("Jacksonville, FL" to "🇺🇸"),
        "954" to ("Fort Lauderdale, FL" to "🇺🇸"),
        "727" to ("St. Petersburg / Clearwater, FL" to "🇺🇸"),
        "561" to ("West Palm Beach, FL" to "🇺🇸"),
        "239" to ("Fort Myers / Cape Coral, FL" to "🇺🇸"),
        "850" to ("Tallahassee / Pensacola, FL" to "🇺🇸"),

        // Other Major US States
        "202" to ("Washington, DC" to "🇺🇸"),
        "404" to ("Atlanta, GA" to "🇺🇸"),
        "678" to ("Atlanta, GA" to "🇺🇸"),
        "770" to ("Atlanta Suburbs, GA" to "🇺🇸"),
        "617" to ("Boston, MA" to "🇺🇸"),
        "857" to ("Boston / Cambridge, MA" to "🇺🇸"),
        "508" to ("Worcester / Cape Cod, MA" to "🇺🇸"),
        "215" to ("Philadelphia, PA" to "🇺🇸"),
        "267" to ("Philadelphia, PA" to "🇺🇸"),
        "412" to ("Pittsburgh, PA" to "🇺🇸"),
        "303" to ("Denver, CO" to "🇺🇸"),
        "720" to ("Denver / Aurora, CO" to "🇺🇸"),
        "719" to ("Colorado Springs, CO" to "🇺🇸"),
        "602" to ("Phoenix, AZ" to "🇺🇸"),
        "480" to ("Scottsdale / Mesa, AZ" to "🇺🇸"),
        "520" to ("Tucson, AZ" to "🇺🇸"),
        "702" to ("Las Vegas, NV" to "🇺🇸"),
        "725" to ("Las Vegas, NV" to "🇺🇸"),
        "775" to ("Reno, NV" to "🇺🇸"),
        "313" to ("Detroit, MI" to "🇺🇸"),
        "248" to ("Troy / Oakland County, MI" to "🇺🇸"),
        "616" to ("Grand Rapids, MI" to "🇺🇸"),
        "612" to ("Minneapolis, MN" to "🇺🇸"),
        "651" to ("St. Paul, MN" to "🇺🇸"),
        "615" to ("Nashville, TN" to "🇺🇸"),
        "901" to ("Memphis, TN" to "🇺🇸"),
        "704" to ("Charlotte, NC" to "🇺🇸"),
        "919" to ("Raleigh / Durham, NC" to "🇺🇸"),
        "216" to ("Cleveland, OH" to "🇺🇸"),
        "614" to ("Columbus, OH" to "🇺🇸"),
        "513" to ("Cincinnati, OH" to "🇺🇸"),
        "317" to ("Indianapolis, IN" to "🇺🇸"),
        "504" to ("New Orleans, LA" to "🇺🇸"),
        "314" to ("St. Louis, MO" to "🇺🇸"),
        "816" to ("Kansas City, MO" to "🇺🇸"),
        "410" to ("Baltimore, MD" to "🇺🇸"),
        "801" to ("Salt Lake City, UT" to "🇺🇸"),
        "808" to ("Honolulu, Hawaii" to "🇺🇸"),
        "907" to ("Anchorage, Alaska" to "🇺🇸"),

        // Canada Provinces
        "416" to ("Toronto, ON" to "🇨🇦"),
        "647" to ("Toronto, ON" to "🇨🇦"),
        "437" to ("Toronto, ON" to "🇨🇦"),
        "905" to ("Greater Toronto / Hamilton, ON" to "🇨🇦"),
        "613" to ("Ottawa, ON" to "🇨🇦"),
        "343" to ("Ottawa, ON" to "🇨🇦"),
        "519" to ("London / Kitchener, ON" to "🇨🇦"),
        "705" to ("Sudbury / Barrie, ON" to "🇨🇦"),
        "514" to ("Montreal, QC" to "🇨🇦"),
        "438" to ("Montreal, QC" to "🇨🇦"),
        "450" to ("Laval / Longueuil, QC" to "🇨🇦"),
        "418" to ("Quebec City, QC" to "🇨🇦"),
        "819" to ("Gatineau / Sherbrooke, QC" to "🇨🇦"),
        "604" to ("Vancouver, BC" to "🇨🇦"),
        "778" to ("Vancouver / Victoria, BC" to "🇨🇦"),
        "250" to ("Victoria / Kelowna, BC" to "🇨🇦"),
        "403" to ("Calgary, AB" to "🇨🇦"),
        "587" to ("Calgary / Edmonton, AB" to "🇨🇦"),
        "780" to ("Edmonton, AB" to "🇨🇦"),
        "204" to ("Winnipeg, MB" to "🇨🇦"),
        "431" to ("Winnipeg, MB" to "🇨🇦"),
        "306" to ("Saskatoon / Regina, SK" to "🇨🇦"),
        "902" to ("Halifax, NS / PEI" to "🇨🇦"),
        "506" to ("Moncton / Saint John, NB" to "🇨🇦"),
        "709" to ("St. John's, NL" to "🇨🇦")
    )

    // UK Geographic & Mobile Codes (+44)
    private val UK_AREA_CODES = mapOf(
        "20" to "London",
        "020" to "London",
        "121" to "Birmingham",
        "0121" to "Birmingham",
        "161" to "Manchester",
        "0161" to "Manchester",
        "141" to "Glasgow",
        "0141" to "Glasgow",
        "151" to "Liverpool",
        "0151" to "Liverpool",
        "113" to "Leeds",
        "0113" to "Leeds",
        "114" to "Sheffield",
        "0114" to "Sheffield",
        "115" to "Nottingham",
        "0115" to "Nottingham",
        "116" to "Leicester",
        "0116" to "Leicester",
        "117" to "Bristol",
        "0117" to "Bristol",
        "131" to "Edinburgh",
        "0131" to "Edinburgh",
        "28" to "Belfast / Northern Ireland",
        "028" to "Belfast / Northern Ireland",
        "29" to "Cardiff / Wales",
        "029" to "Cardiff / Wales"
    )

    // Australia (+61)
    private val AU_AREA_CODES = mapOf(
        "2" to "Sydney / NSW / ACT",
        "02" to "Sydney / NSW / ACT",
        "3" to "Melbourne / VIC / TAS",
        "03" to "Melbourne / VIC / TAS",
        "7" to "Brisbane / QLD",
        "07" to "Brisbane / QLD",
        "8" to "Perth / SA / WA / NT",
        "08" to "Perth / SA / WA / NT"
    )

    // Germany (+49)
    private val DE_AREA_CODES = mapOf(
        "30" to "Berlin",
        "030" to "Berlin",
        "89" to "Munich",
        "089" to "Munich",
        "69" to "Frankfurt",
        "069" to "Frankfurt",
        "40" to "Hamburg",
        "040" to "Hamburg",
        "221" to "Cologne",
        "0221" to "Cologne",
        "711" to "Stuttgart",
        "0711" to "Stuttgart",
        "211" to "Düsseldorf",
        "0211" to "Düsseldorf",
        "341" to "Leipzig",
        "0341" to "Leipzig",
        "351" to "Dresden",
        "0351" to "Dresden",
        "911" to "Nuremberg",
        "0911" to "Nuremberg"
    )

    // France (+33)
    private val FR_AREA_CODES = mapOf(
        "1" to "Paris / Île-de-France",
        "01" to "Paris / Île-de-France",
        "4" to "Southeast (Marseille / Lyon / Nice)",
        "04" to "Southeast (Marseille / Lyon / Nice)",
        "5" to "Southwest (Bordeaux / Toulouse)",
        "05" to "Southwest (Bordeaux / Toulouse)",
        "2" to "Northwest (Nantes / Rennes)",
        "02" to "Northwest (Nantes / Rennes)",
        "3" to "Northeast (Strasbourg / Lille)",
        "03" to "Northeast (Strasbourg / Lille)"
    )

    // Japan (+81)
    private val JP_AREA_CODES = mapOf(
        "3" to "Tokyo",
        "03" to "Tokyo",
        "6" to "Osaka",
        "06" to "Osaka",
        "52" to "Nagoya",
        "052" to "Nagoya",
        "75" to "Kyoto",
        "075" to "Kyoto",
        "45" to "Yokohama",
        "045" to "Yokohama",
        "11" to "Sapporo",
        "011" to "Sapporo",
        "92" to "Fukuoka",
        "092" to "Fukuoka",
        "78" to "Kobe",
        "078" to "Kobe"
    )

    // India (+91)
    private val IN_AREA_CODES = mapOf(
        "11" to "Delhi NCR",
        "011" to "Delhi NCR",
        "22" to "Mumbai, MH",
        "022" to "Mumbai, MH",
        "33" to "Kolkata, WB",
        "033" to "Kolkata, WB",
        "44" to "Chennai, TN",
        "044" to "Chennai, TN",
        "80" to "Bengaluru, KA",
        "080" to "Bengaluru, KA",
        "40" to "Hyderabad, TS",
        "040" to "Hyderabad, TS",
        "79" to "Ahmedabad, GJ",
        "079" to "Ahmedabad, GJ",
        "20" to "Pune, MH",
        "020" to "Pune, MH"
    )

    // Bangladesh (+880) Operators and Landlines
    private val BD_OPERATORS = listOf(
        "017" to "Grameenphone",
        "013" to "Grameenphone",
        "018" to "Robi",
        "016" to "Airtel",
        "019" to "Banglalink",
        "014" to "Banglalink",
        "015" to "Teletalk",
        "02" to "Dhaka Landline",
        "031" to "Chittagong Landline"
    )

    // Pakistan (+92)
    private val PK_OPERATORS = listOf(
        "030" to "Jazz",
        "032" to "Jazz",
        "034" to "Telenor",
        "031" to "Zong",
        "033" to "Ufone",
        "051" to "Islamabad / Rawalpindi",
        "042" to "Lahore",
        "021" to "Karachi"
    )

    // UAE (+971)
    private val AE_OPERATORS = listOf(
        "050" to "Etisalat",
        "054" to "Etisalat",
        "056" to "Etisalat",
        "052" to "du",
        "055" to "du",
        "058" to "du",
        "02" to "Abu Dhabi Landline",
        "04" to "Dubai Landline"
    )

    // Saudi Arabia (+966)
    private val SA_OPERATORS = listOf(
        "050" to "STC",
        "053" to "STC",
        "055" to "STC",
        "054" to "Mobily",
        "056" to "Mobily",
        "058" to "Zain",
        "059" to "Zain",
        "011" to "Riyadh Landline",
        "012" to "Jeddah / Makkah Landline"
    )

    // Nigeria (+234)
    private val NG_OPERATORS = listOf(
        "0803" to "MTN", "0806" to "MTN", "0703" to "MTN", "0706" to "MTN", "0813" to "MTN", "0816" to "MTN", "0903" to "MTN", "0906" to "MTN",
        "0802" to "Airtel", "0808" to "Airtel", "0708" to "Airtel", "0812" to "Airtel", "0902" to "Airtel", "0907" to "Airtel",
        "0805" to "Glo", "0807" to "Glo", "0705" to "Glo", "0815" to "Glo", "0811" to "Glo", "0905" to "Glo",
        "0809" to "9mobile", "0817" to "9mobile", "0818" to "9mobile", "0909" to "9mobile", "0908" to "9mobile"
    )

    // Kenya (+254)
    private val KE_OPERATORS = listOf(
        "070" to "Safaricom", "071" to "Safaricom", "072" to "Safaricom", "079" to "Safaricom", "0110" to "Safaricom", "0111" to "Safaricom",
        "073" to "Airtel", "075" to "Airtel", "078" to "Airtel", "0100" to "Airtel", "0101" to "Airtel",
        "077" to "Telkom"
    )

    // Philippines (+63)
    private val PH_OPERATORS = listOf(
        "0917" to "Globe Telecom", "0927" to "Globe Telecom", "0977" to "Globe Telecom", "0905" to "Globe Telecom", "0906" to "Globe Telecom",
        "0918" to "Smart Communications", "0919" to "Smart Communications", "0920" to "Smart Communications", "0908" to "Smart Communications", "0998" to "Smart Communications",
        "0991" to "DITO", "0992" to "DITO", "0993" to "DITO", "0994" to "DITO"
    )

    fun getCountryInfoByIso(iso: String?): CountryInfo? {
        if (iso.isNullOrBlank()) return null
        return ALL_COUNTRIES.firstOrNull { it.iso.equals(iso.trim(), ignoreCase = true) }
    }

    fun getCountryInfoByDialCode(dialCode: String?): CountryInfo? {
        if (dialCode.isNullOrBlank()) return null
        val clean = dialCode.trim().removePrefix("+")
        return ALL_COUNTRIES.firstOrNull { it.dialCode.removePrefix("+") == clean }
    }

    fun normalizeToE164(clean: String, homeIso: String): String? {
        if (clean.startsWith("+")) return clean
        if (clean.startsWith("00")) return "+" + clean.substring(2)

        val homeCountry = ALL_COUNTRIES.firstOrNull { it.iso.equals(homeIso, ignoreCase = true) }

        // A. Domestic Trunk Dialing (starts with '0' and not '00')
        if (clean.startsWith("0") && clean.length > 2) {
            if (homeCountry != null) {
                return homeCountry.dialCode + clean.removePrefix("0")
            }
            if (clean.startsWith("01") && clean.length == 11) {
                return "+880" + clean.removePrefix("0")
            }
            if (clean.length in 10..11 && (clean.startsWith("02") || clean.startsWith("01") || clean.startsWith("07"))) {
                return "+44" + clean.removePrefix("0")
            }
            if (clean.length in 9..12 && (clean.startsWith("03") || clean.startsWith("04") || clean.startsWith("06") || clean.startsWith("08") || clean.startsWith("015") || clean.startsWith("016") || clean.startsWith("017"))) {
                return "+49" + clean.removePrefix("0")
            }
            if (clean.length == 10 && clean[1] in '1'..'7') {
                return "+33" + clean.removePrefix("0")
            }
            if (clean.length in 10..11 && (clean.startsWith("03") || clean.startsWith("06") || clean.startsWith("090") || clean.startsWith("080") || clean.startsWith("070"))) {
                return "+81" + clean.removePrefix("0")
            }
            if (clean.length == 10 && (clean.startsWith("02") || clean.startsWith("03") || clean.startsWith("07") || clean.startsWith("08") || clean.startsWith("04"))) {
                return "+61" + clean.removePrefix("0")
            }
        }

        // B. Domestic Local Dialing without '0' (User's SIM Country)
        if (homeIso == "US" || homeIso == "CA") {
            if (clean.length == 10 && clean[0] in '2'..'9' && NANP_AREA_CODES.containsKey(clean.substring(0, 3))) {
                return "+1$clean"
            }
            if (clean.length == 11 && clean.startsWith("1") && NANP_AREA_CODES.containsKey(clean.substring(1, 4))) {
                return "+$clean"
            }
        } else if (homeIso == "IN") {
            if (clean.length == 10 && clean[0] in '6'..'9') {
                return "+91$clean"
            }
        } else if (homeIso == "BD") {
            if (clean.length == 10 && clean[0] == '1') {
                return "+880$clean"
            }
        }

        // C. Direct International Dialing Without '+' (e.g. 880..., 44..., 49..., 91..., 33..., 81..., 61...)
        for (country in ALL_COUNTRIES) {
            val dialDigits = country.dialCode.removePrefix("+")
            if (clean.startsWith(dialDigits)) {
                val rest = clean.removePrefix(dialDigits)
                val isPlausible = when {
                    dialDigits == "1" -> clean.length == 11 && rest.length == 10 && rest[0] in '2'..'9'
                    dialDigits.length == 4 -> clean.length == 11
                    dialDigits.length == 3 -> rest.length in 6..11
                    dialDigits.length == 2 -> {
                        // Prevent 10-digit US local numbers from matching 2-digit international prefix when SIM is US/CA
                        if ((homeIso == "US" || homeIso == "CA") && clean.length == 10 && clean[0] in '2'..'9' && NANP_AREA_CODES.containsKey(clean.substring(0, 3))) {
                            false
                        } else {
                            rest.length in 6..11
                        }
                    }
                    else -> rest.length in 6..12
                }
                if (isPlausible) {
                    return "+$clean"
                }
            }
        }

        return null
    }

    fun identifyNumber(rawNumber: String?, userCountryIso: String? = null): CallerIdResult {
        if (rawNumber.isNullOrBlank()) {
            return CallerIdResult(
                category = "Standard",
                regionOrCountry = "Cellular",
                badgeLabel = "CELLULAR",
                flagEmoji = "🌐"
            )
        }

        val clean = rawNumber.trim().replace(Regex("[^0-9+]"), "")
        if (clean.isBlank()) {
            return CallerIdResult(
                category = "Standard",
                regionOrCountry = "Cellular",
                badgeLabel = "CELLULAR",
                flagEmoji = "🌐"
            )
        }

        // 1. Emergency Numbers
        if (clean in EMERGENCY_NUMBERS) {
            return CallerIdResult(
                category = "Emergency",
                regionOrCountry = "Emergency Services",
                isEmergency = true,
                badgeLabel = "EMERGENCY",
                flagEmoji = "🚨"
            )
        }

        // 2. Toll-Free
        for (prefix in TOLL_FREE_PREFIXES) {
            if (clean.startsWith(prefix)) {
                return CallerIdResult(
                    category = "Toll-Free",
                    regionOrCountry = if (prefix.startsWith("080") || prefix.startsWith("+4480")) "UK Toll-Free" else "Toll-Free Helpline",
                    isTollFree = true,
                    badgeLabel = "TOLL-FREE",
                    flagEmoji = "📞",
                    countryName = if (prefix.startsWith("+44") || prefix.startsWith("080")) "United Kingdom" else "United States / Canada"
                )
            }
        }

        val homeIso = userCountryIso?.trim()?.uppercase(java.util.Locale.ROOT)
            ?: SimCountryDetector.currentCountryIso

        // 3. Bangladesh Carrier Detection (Local domestic 01x format for 11 digits or BD home country)
        if (clean.startsWith("01") && clean.length == 11 && (homeIso == "BD" || homeIso.isBlank() || homeIso == "US")) {
            val prefix = clean.substring(0, 3)
            val carrier = BD_OPERATORS.firstOrNull { it.first == prefix }?.second
            if (carrier != null) {
                return CallerIdResult(
                    category = "Mobile Network",
                    regionOrCountry = "Bangladesh • $carrier",
                    badgeLabel = carrier.uppercase(),
                    flagEmoji = "🇧🇩",
                    carrierOrType = carrier,
                    countryName = "Bangladesh",
                    isSimCountry = (homeIso == "BD")
                )
            }
        }

        // 4. Smart International & SIM Location Resolution (without requiring '+' sign)
        val e164 = normalizeToE164(clean, homeIso) ?: (if (clean.startsWith("00")) "+" + clean.substring(2) else clean)

        if (e164.startsWith("+")) {
            val country = ALL_COUNTRIES.firstOrNull { e164.startsWith(it.dialCode) }
            if (country != null) {
                val rest = e164.removePrefix(country.dialCode)
                val isHomeCountry = country.iso.equals(homeIso, ignoreCase = true)

                // Sub-national resolution by Country
                when (country.dialCode) {
                    "+1" -> {
                        if (rest.length >= 3) {
                            val areaCode = rest.substring(0, 3)
                            val area = NANP_AREA_CODES[areaCode]
                            if (area != null) {
                                val (cityName, flag) = area
                                val fullCountry = if (flag == "🇨🇦") "Canada" else "United States"
                                return CallerIdResult(
                                    category = "Regional Location",
                                    regionOrCountry = "$cityName • $fullCountry",
                                    badgeLabel = areaCode,
                                    flagEmoji = flag,
                                    cityOrState = cityName,
                                    countryName = fullCountry
                                )
                            }
                        }
                    }
                    "+44" -> {
                        // Check UK area codes (e.g. 20 for London)
                        val trimmedRest = rest.removePrefix("0")
                        for ((code, city) in UK_AREA_CODES) {
                            val cleanCode = code.removePrefix("0")
                            if (trimmedRest.startsWith(cleanCode)) {
                                return CallerIdResult(
                                    category = "Regional Location",
                                    regionOrCountry = "$city • United Kingdom",
                                    badgeLabel = city.uppercase(),
                                    flagEmoji = "🇬🇧",
                                    cityOrState = city,
                                    countryName = "United Kingdom"
                                )
                            }
                        }
                        if (trimmedRest.startsWith("7")) {
                            return CallerIdResult(
                                category = "Mobile Network",
                                regionOrCountry = "Mobile Network • United Kingdom",
                                badgeLabel = "UK MOBILE",
                                flagEmoji = "🇬🇧",
                                carrierOrType = "Mobile Network",
                                countryName = "United Kingdom"
                            )
                        }
                    }
                    "+61" -> {
                        val trimmedRest = rest.removePrefix("0")
                        for ((code, state) in AU_AREA_CODES) {
                            val cleanCode = code.removePrefix("0")
                            if (trimmedRest.startsWith(cleanCode)) {
                                return CallerIdResult(
                                    category = "Regional Location",
                                    regionOrCountry = "$state • Australia",
                                    badgeLabel = state.uppercase(),
                                    flagEmoji = "🇦🇺",
                                    cityOrState = state,
                                    countryName = "Australia"
                                )
                            }
                        }
                        if (trimmedRest.startsWith("4")) {
                            return CallerIdResult(
                                category = "Mobile Network",
                                regionOrCountry = "Mobile Network • Australia",
                                badgeLabel = "AU MOBILE",
                                flagEmoji = "🇦🇺",
                                carrierOrType = "Mobile Network",
                                countryName = "Australia"
                            )
                        }
                    }
                    "+49" -> {
                        val trimmedRest = rest.removePrefix("0")
                        for ((code, city) in DE_AREA_CODES) {
                            val cleanCode = code.removePrefix("0")
                            if (trimmedRest.startsWith(cleanCode)) {
                                return CallerIdResult(
                                    category = "Regional Location",
                                    regionOrCountry = "$city • Germany",
                                    badgeLabel = city.uppercase(),
                                    flagEmoji = "🇩🇪",
                                    cityOrState = city,
                                    countryName = "Germany"
                                )
                            }
                        }
                        if (trimmedRest.startsWith("15") || trimmedRest.startsWith("16") || trimmedRest.startsWith("17")) {
                            return CallerIdResult(
                                category = "Mobile Network",
                                regionOrCountry = "Mobile Network • Germany",
                                badgeLabel = "DE MOBILE",
                                flagEmoji = "🇩🇪",
                                carrierOrType = "Mobile Network",
                                countryName = "Germany"
                            )
                        }
                    }
                    "+33" -> {
                        val trimmedRest = rest.removePrefix("0")
                        for ((code, region) in FR_AREA_CODES) {
                            val cleanCode = code.removePrefix("0")
                            if (trimmedRest.startsWith(cleanCode)) {
                                return CallerIdResult(
                                    category = "Regional Location",
                                    regionOrCountry = "$region • France",
                                    badgeLabel = region.uppercase(),
                                    flagEmoji = "🇫🇷",
                                    cityOrState = region,
                                    countryName = "France"
                                )
                            }
                        }
                        if (trimmedRest.startsWith("6") || trimmedRest.startsWith("7")) {
                            return CallerIdResult(
                                category = "Mobile Network",
                                regionOrCountry = "Mobile Network • France",
                                badgeLabel = "FR MOBILE",
                                flagEmoji = "🇫🇷",
                                carrierOrType = "Mobile Network",
                                countryName = "France"
                            )
                        }
                    }
                    "+81" -> {
                        val trimmedRest = rest.removePrefix("0")
                        for ((code, city) in JP_AREA_CODES) {
                            val cleanCode = code.removePrefix("0")
                            if (trimmedRest.startsWith(cleanCode)) {
                                return CallerIdResult(
                                    category = "Regional Location",
                                    regionOrCountry = "$city • Japan",
                                    badgeLabel = city.uppercase(),
                                    flagEmoji = "🇯🇵",
                                    cityOrState = city,
                                    countryName = "Japan"
                                )
                            }
                        }
                        if (trimmedRest.startsWith("70") || trimmedRest.startsWith("80") || trimmedRest.startsWith("90")) {
                            return CallerIdResult(
                                category = "Mobile Network",
                                regionOrCountry = "Mobile Network • Japan",
                                badgeLabel = "JP MOBILE",
                                flagEmoji = "🇯🇵",
                                carrierOrType = "Mobile Network",
                                countryName = "Japan"
                            )
                        }
                    }
                    "+91" -> {
                        val trimmedRest = rest.removePrefix("0")
                        for ((code, city) in IN_AREA_CODES) {
                            val cleanCode = code.removePrefix("0")
                            if (trimmedRest.startsWith(cleanCode)) {
                                return CallerIdResult(
                                    category = "Regional Location",
                                    regionOrCountry = "$city • India",
                                    badgeLabel = city.uppercase(),
                                    flagEmoji = "🇮🇳",
                                    cityOrState = city,
                                    countryName = "India"
                                )
                            }
                        }
                        if (trimmedRest.startsWith("6") || trimmedRest.startsWith("7") || trimmedRest.startsWith("8") || trimmedRest.startsWith("9")) {
                            return CallerIdResult(
                                category = "Mobile Network",
                                regionOrCountry = "Mobile Network • India",
                                badgeLabel = "IN MOBILE",
                                flagEmoji = "🇮🇳",
                                carrierOrType = "Mobile Network",
                                countryName = "India"
                            )
                        }
                    }
                    "+880" -> {
                        val trimmedRest = rest.removePrefix("0")
                        for ((prefix, carrier) in BD_OPERATORS) {
                            val cleanPfx = prefix.removePrefix("0")
                            if (trimmedRest.startsWith(cleanPfx)) {
                                return CallerIdResult(
                                    category = "Mobile Network",
                                    regionOrCountry = "Bangladesh • $carrier",
                                    badgeLabel = carrier.uppercase(),
                                    flagEmoji = "🇧🇩",
                                    carrierOrType = carrier,
                                    countryName = "Bangladesh"
                                )
                            }
                        }
                    }
                    "+92" -> {
                        val trimmedRest = rest.removePrefix("0")
                        for ((prefix, op) in PK_OPERATORS) {
                            val cleanPfx = prefix.removePrefix("0")
                            if (trimmedRest.startsWith(cleanPfx)) {
                                return CallerIdResult(
                                    category = "Mobile Network",
                                    regionOrCountry = "$op • Pakistan",
                                    badgeLabel = op.uppercase(),
                                    flagEmoji = "🇵🇰",
                                    carrierOrType = op,
                                    countryName = "Pakistan"
                                )
                            }
                        }
                    }
                    "+971" -> {
                        val trimmedRest = rest.removePrefix("0")
                        for ((prefix, op) in AE_OPERATORS) {
                            val cleanPfx = prefix.removePrefix("0")
                            if (trimmedRest.startsWith(cleanPfx)) {
                                return CallerIdResult(
                                    category = "Mobile Network",
                                    regionOrCountry = "$op • UAE",
                                    badgeLabel = op.uppercase(),
                                    flagEmoji = "🇦🇪",
                                    carrierOrType = op,
                                    countryName = "United Arab Emirates"
                                )
                            }
                        }
                    }
                    "+966" -> {
                        val trimmedRest = rest.removePrefix("0")
                        for ((prefix, op) in SA_OPERATORS) {
                            val cleanPfx = prefix.removePrefix("0")
                            if (trimmedRest.startsWith(cleanPfx)) {
                                return CallerIdResult(
                                    category = "Mobile Network",
                                    regionOrCountry = "$op • Saudi Arabia",
                                    badgeLabel = op.uppercase(),
                                    flagEmoji = "🇸🇦",
                                    carrierOrType = op,
                                    countryName = "Saudi Arabia"
                                )
                            }
                        }
                    }
                    "+234" -> {
                        val trimmedRest = rest.removePrefix("0")
                        for ((prefix, op) in NG_OPERATORS) {
                            val cleanPfx = prefix.removePrefix("0")
                            if (trimmedRest.startsWith(cleanPfx)) {
                                return CallerIdResult(
                                    category = "Mobile Network",
                                    regionOrCountry = "$op • Nigeria",
                                    badgeLabel = op.uppercase(),
                                    flagEmoji = "🇳🇬",
                                    carrierOrType = op,
                                    countryName = "Nigeria"
                                )
                            }
                        }
                    }
                    "+254" -> {
                        val trimmedRest = rest.removePrefix("0")
                        for ((prefix, op) in KE_OPERATORS) {
                            val cleanPfx = prefix.removePrefix("0")
                            if (trimmedRest.startsWith(cleanPfx)) {
                                return CallerIdResult(
                                    category = "Mobile Network",
                                    regionOrCountry = "$op • Kenya",
                                    badgeLabel = op.uppercase(),
                                    flagEmoji = "🇰🇪",
                                    carrierOrType = op,
                                    countryName = "Kenya"
                                )
                            }
                        }
                    }
                    "+63" -> {
                        val trimmedRest = rest.removePrefix("0")
                        for ((prefix, op) in PH_OPERATORS) {
                            val cleanPfx = prefix.removePrefix("0")
                            if (trimmedRest.startsWith(cleanPfx)) {
                                return CallerIdResult(
                                    category = "Mobile Network",
                                    regionOrCountry = "$op • Philippines",
                                    badgeLabel = op.uppercase(),
                                    flagEmoji = "🇵🇭",
                                    carrierOrType = op,
                                    countryName = "Philippines"
                                )
                            }
                        }
                    }
                }

                // General Country Match
                return CallerIdResult(
                    category = "International",
                    regionOrCountry = country.name,
                    badgeLabel = country.name.uppercase(),
                    flagEmoji = country.flag,
                    countryName = country.name,
                    isSimCountry = isHomeCountry
                )
            }
        }

        val isSimNanp = (homeIso == "US" || homeIso == "CA")

        // 5. 10-digit US / Canada standard dialable input fallback (without leading +1)
        if (clean.length == 10 && clean[0] in '2'..'9') {
            val areaCode = clean.substring(0, 3)
            val area = NANP_AREA_CODES[areaCode]
            if (area != null) {
                val (cityName, flag) = area
                val country = if (flag == "🇨🇦") "Canada" else "United States"
                return CallerIdResult(
                    category = "Regional Location",
                    regionOrCountry = "$cityName • $country",
                    badgeLabel = areaCode,
                    flagEmoji = flag,
                    cityOrState = cityName,
                    countryName = country,
                    isSimCountry = isSimNanp
                )
            }
        }

        // 6. 11-digit US format starting with 1
        if (clean.length == 11 && clean.startsWith("1")) {
            val areaCode = clean.substring(1, 4)
            val area = NANP_AREA_CODES[areaCode]
            if (area != null) {
                val (cityName, flag) = area
                val country = if (flag == "🇨🇦") "Canada" else "United States"
                return CallerIdResult(
                    category = "Regional Location",
                    regionOrCountry = "$cityName • $country",
                    badgeLabel = areaCode,
                    flagEmoji = flag,
                    cityOrState = cityName,
                    countryName = country,
                    isSimCountry = isSimNanp
                )
            }
        }

        return CallerIdResult(
            category = "Standard",
            regionOrCountry = "Cellular",
            badgeLabel = "CELLULAR",
            flagEmoji = "🌐"
        )
    }
}
