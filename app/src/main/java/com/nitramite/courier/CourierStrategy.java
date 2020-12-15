package com.nitramite.courier;

import com.nitramite.utils.Locale;

@FunctionalInterface
public interface CourierStrategy {

    ParcelObject execute(final String parcelCode, final Locale locale); // Execute strategy interface

} // End of interface