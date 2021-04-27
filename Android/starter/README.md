# MetaWear Android Starter
This project is a stripped down version of the MetaWear Android app that provides a template for creating a MetaWear Android 
app.  The provided code handles Bluetooth scanning and maintaining an connection to the board; all users need to do is add their 
own UI elements and setup their board with the MetaWear API.

# Usage
User additions will mostly be added to the [DeviceSetupActivityFragment](https://github.com/mbientlab-projects/MetaWear-AndroidStarterApp/blob/master/app/src/main/java/com/mbientlab/metawear/starter/DeviceSetupActivityFragment.java) 
class and the [fragment_device_setup.xml](https://github.com/mbientlab-projects/MetaWear-AndroidStarterApp/blob/master/app/src/main/res/layout/fragment_device_setup.xml) 
layout file.  In the DeviceSetupActivityFragment class, users can use the [reconnected](https://github.com/mbientlab-projects/MetaWear-AndroidStarterApp/blob/master/app/src/main/java/com/mbientlab/metawear/starter/DeviceSetupActivityFragment.java#L105) 
function to be alerted of when connection is re-established.

The next section provides a simple example that shows how to add a switch that controls the LED using this app template.

## LED Switch
In the ``fragment_device_setup.xml`` layout file, add a switch to turn on/off the LED.  

```xml
<Switch
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="LED"
        android:id="@+id/led_ctrl"
        android:layout_alignParentTop="true"
        android:layout_alignParentStart="true"
        android:layout_alignParentEnd="true"
        android:checked="false" />
```

Then, override ``onViewCreated`` to have the switch control the led with an ``OnCheckChangedListener`` class:

```java
@Override
public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    ((Switch) view.findViewById(R.id.led_ctrl)).setOnCheckedChangeListener((buttonView, isChecked) -> {
        Led led= metawear.getModule(Led.class);
        if (isChecked) {
            led.editPattern(Led.Color.BLUE, Led.PatternPreset.SOLID)
                    .repeatCount(Led.PATTERN_REPEAT_INDEFINITELY)
                    .commit();
            led.play();
        } else {
            led.stop(true);
        }
    });
}
```

After making your code changes, load the app on your phone and use the switch to turn on/off the LED.

