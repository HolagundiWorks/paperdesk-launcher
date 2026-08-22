# Recovery and unroot

Before changing the device, retain the exact official firmware for its model, region/CSC, and bootloader revision. Back up user data separately; bootloader unlocking normally wipes it.

To unroot, use Magisk's complete uninstall when the device boots normally. If it does not boot, enter Download Mode using the device-specific hardware key sequence and flash the matching stock AP (and, when necessary, the complete matching BL/AP/CP/CSC set) with a trusted Samsung flashing tool. `CSC` normally wipes data; `HOME_CSC` may preserve it but is not appropriate for every transition.

Never downgrade across Samsung anti-rollback/bootloader revisions. Stop if the model or bootloader binary revision does not match. This utility does not bypass Factory Reset Protection; recover the authorized Google/Samsung account through official channels.
