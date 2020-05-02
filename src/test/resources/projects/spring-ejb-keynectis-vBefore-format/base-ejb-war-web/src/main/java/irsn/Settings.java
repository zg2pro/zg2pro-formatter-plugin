package irsn;


import java.net.URL;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.regex.Pattern;

/**
 * @author samuel.herve
 */
public enum Settings {

	MOCKUP_DATA("mockup.data", "false"),
	TMP_DIR("sigis.tmpdir", System.getProperty("java.io.tmpdir")),
	SIGIS_EMAIL_ADMINADDRESS("sigis.email.adminaddress", "apteagrouptest@gmail.com"),
	IRSN_WEBSITE_URL("irsn.website.url", "http://www.irsn.fr/"),
	SMTP_HOST("sigis.smtp.host", "smtp.gmail.com"),
	SMTP_PORT("sigis.smtp.port", "465"),
	EMAIL_USERNAME("sigis.email.username", "apteagrouptest"),
	EMAIL_PASSWORD("sigis.email.password", "apteagroup"),
	EMAIL_SSL("sigis.email.sslconnect", "true"),
	EMAIL_TLS("sigis.email.tlsconnect", "true"),
	EMAIL_NOREPLY("sigis.email.noreplyaddress", "apteagrouptest@gmail.com"),
	SERVICE_JNDI_HOST("sigis.service.jndi.host", "localhost"),
	SERVICE_JNDI_PORT("sigis.service.jndi.port", "3700"),
	SERVICE_JNDI_TIMEOUT("sigis.service.jndi.timeout", "2000"),
	WEB_CLIENT_REALM("sigis.web.client.realm", "sigis-realm"),
	CUPS_SERVER_HOST("cups.server.host", "localhost"),
	CUPS_SERVER_PORT("cups.server.port", "631"),
	CUPS_SERVER_USER("cups.server.user", "Sigis"),
	CUPS_SERVER_PRINTER_NAME("cups.server.printer.name", "Sigis"),
	DEFAULT_TIMER_CRON("timer.default.cron", "*/5 * * * *"),
	TASK_CREATION_BATCH_SIZE("timer.task.creation.batch.size", "400"),
	TASKS_NB_ITEMS_PER_PAGE("tasks.nb.items.per.page", "100"),
	TRANSFERT_AUTO_CAS_GAMMAGRAPHIE("transfert.auto.cas.gammagraphie", "true"),
	FORMAT_DATE_APPLICATION("sigis.application.date.format", "dd/MM/yyyy"),
	TIME_RECALL_EMAILS_RECOGNITION("sigis.time.emails.recognition", "7"),
	TIME_RECALL_MONTHS_INVENTORIES("sigis.time.inventories.months", "12"),
	TIME_RECALL_MONTHS_BALANCE_SUPPLIERS("sigis.time.balance.suppliers.months", "3"),
	TIME_ALLOWED_EMAILS_CONFIRMATION("sigis.time.emails.confirmation", "30"),
	TIME_AFTER_WHICH_CONTACT_CONSIDERED_POSTED("sigis.time.contacts.month.posted", "1"),
	TIME_MOVES_NO_FORM_VALIDITY("sigis.time.moves.no.form.expire", "6"),
	TIME_DEFAULT_REPLY_EXPECTED_DAYS("sigis.time.contacts.reply.expected", "15"),
	FORM_NUMBER_LIMIT_DISTINCTION_FO_BO("sigis.form.number.distinction.fobo", "2000000"),
	CONSTANTE_DEBIT_LIMITE("constante.debit.limite", "11.3438"),
	CONSTANTE_CONVERSION_COBALT_60("constante.conversion.co60", "0.309E-6"),
	CONSTANTE_CONVERSION_MCI("constante.conversion.mCi", "37"),
	MIN_MONTHS_GAP_EXPORT_ANDRA("exportAndra.dateDebut.nogap", "7"),
	LOG_PATTERN("log.pattern", "[SIGIS %d] %p %c{3} : %m%n"),
	DEFAULT_LOG_LEVEL("log.level.default", "WARN"),
	DIGITAL_SIGNATURE_ID_APPLICATION("digital.signature.id.application", "ZZDEMAV1"),
	DIGITAL_SIGNATURE_ID_APPLICATION_SERVER("digital.signature.id.application.server", "DEMO"),
	DIGITAL_SIGNATURE_ID_ORGANISME("digital.signature.id.organisme", "PDFSMS"),
	DIGITAL_SIGNATURE_AUTHORITY("digital.signature.authority", "KWS_INTEGRATION_CDS"),
	DIGITAL_SIGNATURE_SUBMIT_URL("digital.signature.url", "https://keynectis.kwebsign.net/QS/Page1V2"),
	SEARCH_NB_ITEMS_PER_PAGE("search.nbitemsperpage", "20"),
	SEARCH_SKIP_PAGINATION_MAX_NB_PAGES("search.skipmaxnbpages", "1500"),
	// Session : 10 heures par défaut
	SIGIS_SESSION_MAX_AGE_SECONDS("web.session.max.age.seconds", "36000"),
	ANTISPAMLATENCY("fo.spam.temps.min.before.submit", "10000");
	
	final static Pattern extractCustomLevel = Pattern.compile("log.level.(.*)");
	final static Pattern extractCustomAppender = Pattern.compile("log.appender.(.*)");
	
	public static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat();

	static {
		DecimalFormatSymbols symbols = new DecimalFormatSymbols();
		symbols.setGroupingSeparator(' ');
		symbols.setDecimalSeparator(',');
		DECIMAL_FORMAT.setDecimalFormatSymbols(symbols);
		DECIMAL_FORMAT.setGroupingSize(3);
		DECIMAL_FORMAT.setMaximumFractionDigits(Integer.MAX_VALUE);
	}
	private String key;
	private String defaultValue;

	private Settings(String key, String defaultValue) {
		this.key = key;
		this.defaultValue = defaultValue;
	}

	public String getKey() {
		return key;
	}

	/**
	 * @return setting value.
	 */
	public String getSetting() {
		return defaultValue;
	}

	public int getSettingAsInt() {
		return Integer.parseInt(getSetting());
	}

	public long getSettingAsLong() {
		return Long.parseLong(getSetting());
	}

	public boolean getSettingAsBool() {
		return Boolean.parseBoolean(getSetting());
	}

	

	private static void showErrorMessage() {
		StringBuilder message = new StringBuilder("La variable d'environnement SIGIS_SETTINGS_FILE doit référencer un fichier contenant les paramètres suivants (les valeurs par défaut sont indiquées ici) : ");
		for (Settings setting : Settings.values()) {
			message.append("\n");
			message.append(setting.key);
			message.append("=");
			message.append(setting.defaultValue);
		}
		System.err.println(message.toString());
	}

	/**
	 * Alias to getSetting().
	 *
	 * @return this.getSetting().
	 */
	@Override
	public String toString() {
		return this.getSetting();
	}
/*
	public static Map<String, String> listCustomLoggingSettings() {
		Map<String, String> result = new TreeMap<String, String>();
		for (String key : customLevels.keySet()) {
			result.put(String.format("custom log level (log.level.%s)", key), customLevels.get(key).toString());
		}
		for (String key : customAppenders.keySet()) {
			result.put(String.format("custom log appender (log.appender.%s)", key), customAppenders.get(key));
		}
		return result;
	}
	private static Map<String, Level> customLevels = new HashMap<String, Level>();
	private static Map<String, String> customAppenders = new HashMap<String, String>();
*/
	

	public static String getCurrentSigisRevision() {
		try {
			Class clazz = Settings.class;
			String className = clazz.getSimpleName() + ".class";
			String classPath = clazz.getResource(className).toString();
			if (!classPath.startsWith("jar")) {
				return "";
			}
			String manifestPath = classPath.substring(0, classPath.lastIndexOf("!") + 1) + "/META-INF/MANIFEST.MF";
			Manifest manifest = new Manifest(new URL(manifestPath).openStream());
			Attributes attr = manifest.getMainAttributes();
			return attr.getValue("Implementation-Build");
		} catch (Exception e) {
			return "";
		}
	}
}
