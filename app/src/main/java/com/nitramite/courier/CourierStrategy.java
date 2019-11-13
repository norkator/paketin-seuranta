package com.nitramite.courier;

@FunctionalInterface
public interface CourierStrategy {

    ParcelObject execute(final String parcelCode); // Execute strategy interface

} // End of interface