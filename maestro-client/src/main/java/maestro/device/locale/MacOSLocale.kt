package maestro.device.locale

import maestro.device.Platform

/**
 * macOS device locale - fixed enum of supported locale combinations.
 */
enum class MacOSLocale(override val code: String) : DeviceLocale {
  EN_US("en_US");

  override val displayName: String
    get() = DeviceLocale.getDisplayNameFromCode(code)

  override val languageCode: String
    get() {
      val parts = code.split("_", "-")
      return parts[0]
    }

  override val countryCode: String
    get() = code.split("_", "-")[1]

  override val platform: Platform = Platform.MACOS

  companion object {
    /**
     * Gets all locale codes as a set.
     */
    val allCodes: Set<String>
      get() = entries.map { it.code }.toSet()

    /**
     * Finds a locale by its string representation.
     * Accepts both underscore and hyphen formats.
     *
     * @throws LocaleValidationException if not found
     */
    fun fromString(localeString: String): MacOSLocale {
      return entries.find {
        it.code == localeString ||
                it.code.replace("_", "-") == localeString ||
                it.code.replace("-", "_") == localeString
      } ?: throw LocaleValidationException("Failed to validate macOS device locale. Here is a full list of supported locales: \n\n ${allCodes.joinToString(", ")}")
    }

    /**
     * Validates if a locale string is valid for macOS.
     */
    fun isValid(localeString: String): Boolean {
      return entries.any {
        it.code == localeString ||
                it.code.replace("_", "-") == localeString ||
                it.code.replace("-", "_") == localeString
      }
    }

    /**
     * Finds a locale code given language and country codes.
     * Tries both underscore and hyphen formats.
     * @return Locale code if found (e.g., "en_US" or "en-US"), null otherwise
     */
    fun find(languageCode: String, countryCode: String): String? {
      // Try underscore format first
      val underscoreFormat = "${languageCode}_$countryCode"
      if (isValid(underscoreFormat)) {
        return underscoreFormat
      }

      // Try hyphen format
      val hyphenFormat = "$languageCode-$countryCode"
      if (isValid(hyphenFormat)) {
        return hyphenFormat
      }

      return null
    }
  }
}
