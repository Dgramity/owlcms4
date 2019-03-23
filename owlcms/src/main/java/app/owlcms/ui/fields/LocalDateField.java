/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Apache 2.0 License amended with the
 * Commons Clause.
 * License text at https://github.com/jflamy/owlcms4/master/License
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */
package app.owlcms.ui.fields;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.FormatStyle;
import java.util.Locale;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.converter.Converter;
import com.vaadin.flow.data.renderer.LocalDateRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.function.ValueProvider;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * LocalDate field with conversion, validation and rendering.
 * 
 * @author Jean-François Lamy
 *
 */
@SuppressWarnings("serial")
public class LocalDateField extends WrappedTextField<LocalDate> {
	
	@Override
	protected void initLoggers() {
		logger = (Logger)LoggerFactory.getLogger(LocalDateField.class);
		logger.setLevel(Level.DEBUG);
	}

	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
	

	@Override
	public Converter<String, LocalDate> getConverter() {
		return new Converter<String,LocalDate>() {		
			@Override
			public String convertToPresentation(LocalDate value, ValueContext context) {
				Locale locale = context.getLocale().orElse(Locale.ENGLISH);
				return (value != null ? FORMATTER.withLocale(locale).format(value) : "");
			}

			@Override
			public Result<LocalDate> convertToModel(String value, ValueContext context) {
				Locale locale = context.getLocale().orElse(Locale.ENGLISH);
				return doParse(value, locale, FORMATTER.withLocale(locale));
			}
		};
	}

	public static <SOURCE> Renderer<SOURCE> getRenderer(ValueProvider<SOURCE, LocalDate> v, Locale locale) {
		return new LocalDateRenderer<SOURCE>(v, FORMATTER.withLocale(locale));
	}
	
	@Override
	public String toString() {
		return FORMATTER.format(getValue());
	}
	
	@Override
	protected String invalidFormatErrorMessage(Locale locale) {
		LocalDate sampleDate = LocalDate.of(2000, 12, 31);
		return "Date must be in international format YYYY-MM-DD "
				+ "("
				+ FORMATTER.format(sampleDate)
				+ " for "
				+ DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
					.format(sampleDate)
				+ ")";
	}
	
	private Result<LocalDate> doParse(String content, Locale locale, DateTimeFormatter formatter) {
		LocalDate parse;
		try {
			if ((content == null || content.trim().isEmpty()) && !this.isRequired()) {
				// field is not required, accept empty content
				setFormatValidationStatus(true, locale);
				return Result.ok(null);
			}
			parse = LocalDate.parse(content, formatter);
			setFormatValidationStatus(true, locale);
			return Result.ok(parse);
		} catch (DateTimeParseException e) {
			String m = invalidFormatErrorMessage(locale);
			setFormatValidationStatus(false, locale);
			return Result.error(m);
		}
	}

}

