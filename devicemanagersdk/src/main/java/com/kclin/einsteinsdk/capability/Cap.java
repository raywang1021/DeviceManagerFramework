package com.kclin.einsteinsdk.capability;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by davidliu on 3/25/15.
 */
public class Cap implements Parcelable {
    public int id;
    public String component;
    public String capability;

    public Cap(int id, String com, String cap) {
        this.id = id;
        this.component = com;
        this.capability = cap;
    }

    protected Cap(Parcel in) {
        id = in.readInt();
        component = in.readString();
        capability = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(component);
        dest.writeString(capability);
    }

    @SuppressWarnings("unused")
    public static final Creator<Cap> CREATOR = new Creator<Cap>() {
        @Override
        public Cap createFromParcel(Parcel in) {
            return new Cap(in);
        }

        @Override
        public Cap[] newArray(int size) {
            return new Cap[size];
        }
    };
}