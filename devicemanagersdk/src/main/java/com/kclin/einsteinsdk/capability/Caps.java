package com.kclin.einsteinsdk.capability;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by davidliu on 3/25/15.
 */
public class Caps implements Parcelable {
    public List<Cap> caps;

    public Caps() {
        caps = new ArrayList<Cap>();
    }

    public void add(Cap cap) {
        caps.add(cap);
    }

    public void remove(Cap cap) {
        caps.remove(cap);
    }

    protected Caps(Parcel in) {
        if (in.readByte() == 0x01) {
            caps = new ArrayList<Cap>();
            in.readList(caps, Cap.class.getClassLoader());
        } else {
            caps = null;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        if (caps == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeList(caps);
        }
    }

    @SuppressWarnings("unused")
    public static final Creator<Caps> CREATOR = new Creator<Caps>() {
        @Override
        public Caps createFromParcel(Parcel in) {
            return new Caps(in);
        }

        @Override
        public Caps[] newArray(int size) {
            return new Caps[size];
        }
    };
}