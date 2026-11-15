# EasyStart Call – SMS protocol

[Suomi](../SMS-PROTOKOLLA.md) · **English**

Source: Eberspächer EasyStart Call user manual (FI, 08.2014). Implementation: [`Commands.kt`](../../app/src/main/java/fi/heatcall/sms/Commands.kt) and [`ReplyParser.kt`](../../app/src/main/java/fi/heatcall/sms/ReplyParser.kt).

## General

- Format: `pin:<PIN> <command>`. There is **always a space** between the PIN code and the command.
- The device accepts both upper and lower case letters.
- Command parameters are separated by commas, for example `heater:on,run:30`.
- If the sender's number is authorised, the PIN code is not needed. HeatCall still always sends it.
- The device sends a reply only if **confirmation messages** are on. It always replies to queries (`?`).

## Heating

| Function | Command | Reply (example) |
|---|---|---|
| Heating on (latest settings) | `pin:1234 heater:on` | `Heater:on,mode:heater,run:45` |
| Heating on, run time | `pin:1234 heater:on,run:30` | `Heater:on,mode:heater,run:30` |
| Continuous operation (air heater only) | `pin:1234 heater:on,run:perm,sp:24` | `Heater:on,mode:heater,run:perm,SP:24` |
| Ventilation on | `pin:1234 heater:on,mode:fan,run:30` | `Heater:on,mode:fan,run:30` |
| Off | `pin:1234 heater:off` | `Heater:off` |
| Status query | `pin:1234 heater?` | `Heater:on,mode:heater,run:45` |
| Interior temperature | `pin:1234 temp?` | `Temp:ok,value:28` |

- `run`: 10–120 minutes or `perm` (continuous operation, air heaters only).
- `sp`: set temperature 8–36 °C (air heaters only). Single-digit values have no leading zero (`9`, not `09`).

## Timers

| Function | Command | Reply |
|---|---|---|
| Programming | `pin:1234 t1:on,time:MO-FR.06.30,run:30` | `T1:on,time:MO-FR.06.30,mode:heater,run:30` |
| Programming, ventilation | `pin:1234 t2:on,time:FR.16.30,mode:fan,run:40` | `T2:on,time:FR.16.30,mode:fan,run:40` |
| Activation (with saved values) | `pin:1234 t1:on` | `T1:on,…` |
| Deactivation | `pin:1234 t1:off` | `T1:off` |
| Status of one timer | `pin:1234 t1?` | `T1:on,time:…` or `T1:off` |
| Status of all timers | `pin:1234 tall?` | `T1:…,T2:…,T3:…` |

- Timer: `t1`, `t2` or `t3`.
- `time`: `<DAY>.<HH>.<MM>`. The day is `MO TU WE TH FR SA SU` or a range `MO-FR`, `SA-SU`, `MO-SU`.
- By default the time is the **departure time** (end): the heater starts the run time earlier and switches off at the set time. Start time mode (start) is set during installation.
- Continuous operation is not possible with a timer.

## Device

| Function | Command | Reply |
|---|---|---|
| Clock query | `pin:1234 devicetime?` | `DeviceTime:ok,time:TU.14.20` |
| Clock setting | `pin:1234 devicetime:set,time:FR.06.30` | `DeviceTime:ok,time:FR.06.30` |
| Authorised number | `pin:1234 mainphone?` | `MainPhone:ok,number:+358…` |
| Authorise the sender | `pin:1234 mainphone:set,number:mine` | `MainPhone:ok,number:+358…` |
| Authorise a number | `pin:1234 mainphone:set,number:+358401234567` | `MainPhone:ok,number:…` |
| PIN code change | `pin:1234 config:set,pin:9876` | – |

The device clock and weekday must be set again after every power cut. Summer and winter time must also be changed by hand, because the device does not know about time zones. HeatCall (`DeviceClock.kt`) builds the set command from the phone's local time, rounded to the nearest minute (the format on page 27 of the manual, `pin:1234 devicetime:set,time:FR.06.30`, verified by a unit test). It calculates the device's offset from the clock query reply (wrapping around the week) and shows a reminder for three weeks after a time change.

## Error messages

Error messages are sent only if `ErrorFeedback:on` was set during installation.

| Reply | Meaning |
|---|---|
| `ErD:HeaterCom:lost,Heater:off` | Communication with the heater was lost |
| `ErD:HeaterCom:no_heater_found` | The heater was not detected during setup |
| `ErL:DatCom:lost` | Wiring fault |
| `ErH:Heater:Error` / `ErH:HeaterError` | The heater has a fault |
| `ErH:Heater:mode_not_supported` | The heater does not support the requested function |
| `ErC:Temp:missing` | No temperature sensor is installed |
| `ErC:Temp:short_circuit` | The temperature sensor is faulty |
| `ErC:DeviceTime:time_not_set` | The device clock has not been set |
| `ErR:DeviceTime:time_not_set,T2:off` | The timer was not activated because the clock has not been set |

## How HeatCall interprets replies

`ReplyParser` splits the reply at the commas into `key:value` pairs and builds a title and lines from them in the app's language (Finnish or English). Unrecognised parts are shown as they are, so no information is lost. The heater state (`Heater:on/off`) is saved as the device's latest known state, which is shown on the card and in the widget.
