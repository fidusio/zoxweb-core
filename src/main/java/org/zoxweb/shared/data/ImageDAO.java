package org.zoxweb.shared.data;

import org.zoxweb.shared.util.GetNVConfig;
import org.zoxweb.shared.util.GetValue;
import org.zoxweb.shared.util.NVConfig;
import org.zoxweb.shared.util.NVConfigEntity;
import org.zoxweb.shared.util.NVConfigEntityPortable;
import org.zoxweb.shared.util.NVConfigManager;
import org.zoxweb.shared.util.SharedUtil;
import org.zoxweb.shared.filters.FilterType;

/**
 * Created on 7/22/17
 */
@SuppressWarnings("serial")
public class ImageDAO
    extends TimeStampDAO
{

    public enum ImageFormat
        implements GetValue<String> {

        IMAGE_GIF("image/gif"),
        IMAGE_PNG("image/png"),
        IMAGE_JPEG("image/jpeg"),
        IMAGE_BMP("image/bmp"),
        IMAGE_WEBP("image/webp")

        ;

        private String value;

        ImageFormat(String value) {
            this.value = value;
        }

        @Override
        public String getValue() {
            return value;
        }
    }

    public enum Param
        implements GetNVConfig
    {
        LENGTH(NVConfigManager.createNVConfig("length", "Image length in pixels", "Length", false, false, long.class)),
        WIDTH(NVConfigManager.createNVConfig("width", "Image width in pixels", "Width", false, false, long.class)),
        SIZE(NVConfigManager.createNVConfig("size", "Image size in bytes", "Size", true, false, long.class)),
        FORMAT(NVConfigManager.createNVConfig("format", "Image format type", "Format", true, false, ImageFormat.class)),
        RESOURCE_LOCATOR(NVConfigManager.createNVConfig("resource_locator", "Resource locator", "ResourceLocator", true, false, false, String.class, FilterType.URL)),

        ;

        private final NVConfig nvc;

        Param(NVConfig nvc)
        {
            this.nvc = nvc;
        }

        public NVConfig getNVConfig()
        {
            return nvc;
        }
    }

    public static final NVConfigEntity NVC_IMAGE_DAO = new NVConfigEntityPortable(
            "image_dao",
            null ,
            "ImageDAO",
            true,
            false,
            false,
            false,
            ImageDAO.class,
            SharedUtil.extractNVConfigs(Param.values()),
            null,
            false,
            TimeStampDAO.NVC_TIME_STAMP_DAO
    );

    public ImageDAO()
    {
        super(NVC_IMAGE_DAO);
    }

    public long getLength()
    {
        return lookupValue(Param.LENGTH);
    }

    public void setLength(long length)
    {
        setValue(Param.LENGTH, length);
    }

    public long getWidth()
    {
        return lookupValue(Param.WIDTH);
    }

    public void setWidth(long width)
    {
        setValue(Param.WIDTH, width);
    }

    public long getSize()
    {
        return lookupValue(Param.SIZE);
    }

    public void setSize(long size)
    {
        setValue(Param.SIZE, size);
    }

    public ImageFormat getFormat()
    {
        return lookupValue(Param.FORMAT);
    }

    public void setFormat(ImageFormat format)
    {
        setValue(Param.FORMAT, format);
    }

    public String getResourceLocator()
    {
        return lookupValue(Param.RESOURCE_LOCATOR);
    }

    public void setResourceLocator(String locator)
    {
        setValue(Param.RESOURCE_LOCATOR, locator);
    }

}