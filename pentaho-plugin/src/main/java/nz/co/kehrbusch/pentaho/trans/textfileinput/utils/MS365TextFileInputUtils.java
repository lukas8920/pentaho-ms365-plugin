package nz.co.kehrbusch.pentaho.trans.textfileinput.utils;

import nz.co.kehrbusch.ms365.interfaces.entities.IStreamProvider;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleFileException;
import org.pentaho.di.core.logging.LogChannelInterface;
import org.pentaho.di.core.row.RowDataUtil;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.row.value.ValueMetaBase;
import org.pentaho.di.core.util.EnvUtil;
import org.pentaho.di.core.util.Utils;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.step.errorhandling.FileErrorHandler;
import org.pentaho.di.trans.steps.file.BaseFileField;
import org.pentaho.di.trans.steps.file.BaseFileInputAdditionalField;
import org.pentaho.di.trans.steps.fileinput.text.*;

import java.util.Date;

import static org.pentaho.di.trans.steps.fileinput.text.TextFileInputUtils.checkPattern;
import static org.pentaho.di.trans.steps.fileinput.text.TextFileInputUtils.convertLineToStrings;

public class MS365TextFileInputUtils {
    private static final Class<?> PKG = MS365TextFileInputUtils.class;

    public static final MS365TextLine getLine(LogChannelInterface log, BufferedInputStreamReader reader, EncodingType encodingType, int fileFormatType, StringBuilder line, String regex, long lineNumberInFile) throws KettleFileException {
        return getLine(log, reader, encodingType, fileFormatType, line, regex, "", lineNumberInFile);
    }

    public static final MS365TextLine getLine(LogChannelInterface log, BufferedInputStreamReader reader, EncodingType encodingType, int fileFormatType, StringBuilder line, String regex, String escapeChar, long lineNumberInFile) throws KettleFileException {
        String sline = TextFileInputUtils.getLine(log, reader, encodingType, fileFormatType, line);
        boolean lenientEnclosureHandling = ValueMetaBase.convertStringToBoolean(Const.NVL(EnvUtil.getSystemProperty("KETTLE_COMPATIBILITY_TEXT_FILE_INPUT_USE_LENIENT_ENCLOSURE_HANDLING"), "N"));
        if (!lenientEnclosureHandling && sline != null) {
            StringBuilder sb;
            for(sb = new StringBuilder(sline); checkPattern(sb.toString(), regex, escapeChar) % 2 != 0; ++lineNumberInFile) {
                sline = TextFileInputUtils.getLine(log, reader, encodingType, fileFormatType, line);
                if (sline == null) {
                    return new MS365TextLine(sline, lineNumberInFile, (IStreamProvider) null);
                }

                sb.append("\n" + sline);
            }

            return new MS365TextLine(sb.toString(), lineNumberInFile, (IStreamProvider) null);
        } else {
            return new MS365TextLine(sline, lineNumberInFile, (IStreamProvider) null);
        }
    }

    public static final Object[] convertLineToRow(LogChannelInterface log, MS365TextLine textFileLine, TextFileInputMeta info, Object[] passThruFields, int nrPassThruFields, RowMetaInterface outputRowMeta, RowMetaInterface convertRowMeta, String fname, long rowNr, String delimiter, String enclosure, String escapeCharacter, FileErrorHandler errorHandler, BaseFileInputAdditionalField additionalOutputFields, String shortFilename, String path, boolean hidden, Date modificationDateTime, String uri, String rooturi, String extension, Long size) throws KettleException {
        return convertLineToRow(log, textFileLine, info, passThruFields, nrPassThruFields, outputRowMeta, convertRowMeta, fname, rowNr, delimiter, enclosure, escapeCharacter, errorHandler, additionalOutputFields, shortFilename, path, hidden, modificationDateTime, uri, rooturi, extension, size, true);
    }

    public static final Object[] convertLineToRow(LogChannelInterface log, MS365TextLine textFileLine, TextFileInputMeta info, Object[] passThruFields, int nrPassThruFields, RowMetaInterface outputRowMeta, RowMetaInterface convertRowMeta, String fname, long rowNr, String delimiter, String enclosure, String escapeCharacter, FileErrorHandler errorHandler, BaseFileInputAdditionalField additionalOutputFields, String shortFilename, String path, boolean hidden, Date modificationDateTime, String uri, String rooturi, String extension, Long size, boolean failOnParseError) throws KettleException {
        if (textFileLine != null && textFileLine.line != null) {
            Object[] r = RowDataUtil.allocateRowData(outputRowMeta.size());
            int nrfields = info.inputFields.length;
            Long errorCount = null;
            if (info.errorHandling.errorIgnored && info.getErrorCountField() != null && info.getErrorCountField().length() > 0) {
                errorCount = new Long(0L);
            }

            String errorFields = null;
            if (info.errorHandling.errorIgnored && info.getErrorFieldsField() != null && info.getErrorFieldsField().length() > 0) {
                errorFields = "";
            }

            String errorText = null;
            if (info.errorHandling.errorIgnored && info.getErrorTextField() != null && info.getErrorTextField().length() > 0) {
                errorText = "";
            }

            try {
                String[] strings = convertLineToStrings(log, textFileLine.line, info, delimiter, enclosure, escapeCharacter);
                int shiftFields = passThruFields == null ? 0 : nrPassThruFields;

                int fieldnr;
                for(fieldnr = 0; fieldnr < nrfields; ++fieldnr) {
                    BaseFileField f = info.inputFields[fieldnr];
                    int valuenr = shiftFields + fieldnr;
                    ValueMetaInterface valueMeta = outputRowMeta.getValueMeta(valuenr);
                    ValueMetaInterface convertMeta = convertRowMeta.getValueMeta(valuenr);
                    String nullif = fieldnr < nrfields ? f.getNullString() : "";
                    String ifnull = fieldnr < nrfields ? f.getIfNullValue() : "";
                    int trim_type = fieldnr < nrfields ? f.getTrimType() : 0;
                    Object value;
                    if (fieldnr < strings.length) {
                        String pol = strings[fieldnr];

                        try {
                            if (valueMeta.isNull(pol) || !Utils.isEmpty(nullif) && nullif.equals(pol)) {
                                pol = null;
                            }

                            value = valueMeta.convertDataFromString(pol, convertMeta, nullif, ifnull, trim_type);
                        } catch (Exception var44) {
                            if (failOnParseError) {
                                String message = BaseMessages.getString(PKG, "TextFileInput.Log.CoundNotParseField", new String[]{valueMeta.toStringMeta(), "" + pol, valueMeta.getConversionMask(), "" + rowNr});
                                if (!info.errorHandling.errorIgnored) {
                                    throw new KettleException(message, var44);
                                }

                                log.logDetailed(fname, new Object[]{BaseMessages.getString(PKG, "TextFileInput.Log.Warning", new String[0]) + ": " + message + " : " + var44.getMessage()});
                                value = null;
                                if (errorCount != null) {
                                    errorCount = new Long(errorCount + 1L);
                                }

                                StringBuilder sb;
                                if (errorFields != null) {
                                    sb = new StringBuilder(errorFields);
                                    if (sb.length() > 0) {
                                        sb.append("\t");
                                    }

                                    sb.append(valueMeta.getName());
                                    errorFields = sb.toString();
                                }

                                if (errorText != null) {
                                    sb = new StringBuilder(errorText);
                                    if (sb.length() > 0) {
                                        sb.append(Const.CR);
                                    }

                                    sb.append(message);
                                    errorText = sb.toString();
                                }

                                if (errorHandler != null) {
                                    errorHandler.handleLineError(textFileLine.lineNumber, "NO_PARTS");
                                }

                                if (info.isErrorLineSkipped()) {
                                    r = null;
                                }
                            } else {
                                value = pol;
                            }
                        }
                    } else {
                        value = null;
                    }

                    if (r != null) {
                        r[valuenr] = value;
                    }
                }

                if (r != null) {
                    int index;
                    if (fieldnr < nrfields) {
                        for(index = fieldnr; index < info.inputFields.length; ++index) {
                            r[shiftFields + index] = null;
                        }
                    }

                    index = shiftFields + nrfields;
                    if (errorCount != null) {
                        r[index] = errorCount;
                        ++index;
                    }

                    if (errorFields != null) {
                        r[index] = errorFields;
                        ++index;
                    }

                    if (errorText != null) {
                        r[index] = errorText;
                        ++index;
                    }

                    if (info.content.includeFilename) {
                        r[index] = fname;
                        ++index;
                    }

                    if (info.content.includeRowNumber) {
                        r[index] = new Long(rowNr);
                        ++index;
                    }

                    if (additionalOutputFields.shortFilenameField != null) {
                        r[index] = shortFilename;
                        ++index;
                    }

                    if (additionalOutputFields.extensionField != null) {
                        r[index] = extension;
                        ++index;
                    }

                    if (additionalOutputFields.pathField != null) {
                        r[index] = path;
                        ++index;
                    }

                    if (additionalOutputFields.sizeField != null) {
                        r[index] = size;
                        ++index;
                    }

                    if (additionalOutputFields.hiddenField != null) {
                        r[index] = hidden;
                        ++index;
                    }

                    if (additionalOutputFields.lastModificationField != null) {
                        r[index] = modificationDateTime;
                        ++index;
                    }

                    if (additionalOutputFields.uriField != null) {
                        r[index] = uri;
                        ++index;
                    }

                    if (additionalOutputFields.rootUriField != null) {
                        r[index] = rooturi;
                        ++index;
                    }
                }
            } catch (Exception var45) {
                throw new KettleException(BaseMessages.getString(PKG, "TextFileInput.Log.Error.ErrorConvertingLineText", new String[0]), var45);
            }

            if (r != null && passThruFields != null) {
                for(int i = 0; i < nrPassThruFields; ++i) {
                    r[i] = passThruFields[i];
                }
            }

            return r;
        } else {
            return null;
        }
    }
}
