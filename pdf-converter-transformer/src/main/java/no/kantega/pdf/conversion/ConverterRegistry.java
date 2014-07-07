package no.kantega.pdf.conversion;

import no.kantega.pdf.api.DocumentType;
import no.kantega.pdf.throwables.ConversionInputException;
import no.kantega.pdf.throwables.ConverterException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkState;

class ConverterRegistry {

    private final Map<ConversionPath, IExternalConverter> converterMapping;

    public ConverterRegistry(Set<? extends IExternalConverter> externalConverters) {
        converterMapping = new HashMap<ConversionPath, IExternalConverter>();
        for (IExternalConverter externalConverter : externalConverters) {
            converterMapping.putAll(resolve(externalConverter));
        }
    }

    private static Map<ConversionPath, IExternalConverter> resolve(IExternalConverter externalConverter) {
        boolean viableConversionPresent = externalConverter.getClass().isAnnotationPresent(ViableConversion.class);
        boolean viableConversionsPresent = externalConverter.getClass().isAnnotationPresent(ViableConversions.class);
        checkState(viableConversionPresent ^ viableConversionsPresent, externalConverter + " must be annotated with " +
                "exactly one of @ViableConversion or @ViableConversions");
        ViableConversion[] viableConversions = viableConversionPresent
                ? new ViableConversion[]{externalConverter.getClass().getAnnotation(ViableConversion.class)}
                : externalConverter.getClass().getAnnotation(ViableConversions.class).value();
        Map<ConversionPath, IExternalConverter> conversionMapping = new HashMap<ConversionPath, IExternalConverter>();
        for (ViableConversion viableConversion : viableConversions) {
            for (String sourceFormat : viableConversion.from()) {
                for (String targetFormat : viableConversion.to()) {
                    conversionMapping.put(new ConversionPath(new DocumentType(sourceFormat), new DocumentType(targetFormat)), externalConverter);
                }
            }
        }
        return conversionMapping;
    }

    public IExternalConverter lookup(DocumentType sourceFormat, DocumentType targetFormat) {
        IExternalConverter externalConverter = converterMapping.get(new ConversionPath(sourceFormat, targetFormat));
        if (externalConverter == null) {
            throw new ConversionInputException("No converter for conversion of " + sourceFormat + " to " + targetFormat + " available");
        }
        return externalConverter;
    }

    public boolean isOperational() {
        for (IExternalConverter externalConverter : new HashSet<IExternalConverter>(converterMapping.values())) {
            if (!externalConverter.isOperational()) {
                return false;
            }
        }
        return true;
    }

    public void shutDown() {
        Set<RuntimeException> runtimeExceptions = new HashSet<RuntimeException>();
        for (IExternalConverter externalConverter : new HashSet<IExternalConverter>(converterMapping.values())) {
            try {
                externalConverter.shutDown();
            } catch (RuntimeException e) {
                runtimeExceptions.add(e);
            }
        }
        if (runtimeExceptions.size() > 0) {
            throw new ConverterException("Could not shut down at least one external converter: " + runtimeExceptions);
        }
    }

    private static class ConversionPath {

        private final DocumentType sourceFormat;
        private final DocumentType targetFormat;

        private ConversionPath(DocumentType sourceFormat, DocumentType targetFormat) {
            this.sourceFormat = sourceFormat;
            this.targetFormat = targetFormat;
        }

        public DocumentType getSourceFormat() {
            return sourceFormat;
        }

        public DocumentType getTargetFormat() {
            return targetFormat;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null || getClass() != other.getClass()) return false;
            ConversionPath conversionPath = (ConversionPath) other;
            return sourceFormat.equals(conversionPath.sourceFormat) && targetFormat.equals(conversionPath.targetFormat);
        }

        @Override
        public int hashCode() {
            int result = sourceFormat.hashCode();
            result = 31 * result + targetFormat.hashCode();
            return result;
        }
    }
}