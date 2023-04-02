package org.birkl.panama.winmessage;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

public class MessageBox {

    public static void main(String[] args) throws Throwable {

        final String message = """
                This MessageBox was created from Java '%s' version '%s' leveraging the power of Project Panama by using the WIN32 API.
                
                Now also with Unicode 🤗🎂🌈❤️
                """.formatted(System.getProperty("java.vm.name"), System.getProperty("java.vm.version"));

        ReturnValue result = show(0,
                message,
                "This is a native MessageBox!",
                Buttons.MB_OK,
                Icon.MB_ICONINFORMATION);

        System.out.println("MessageBoxW returned: " + result);
    }

    static {
        System.loadLibrary("User32");
    }

    private static final FunctionDescriptor messageBoxWFD = FunctionDescriptor.of(
            // return value
            ValueLayout.JAVA_LONG,

            // arguments
            ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG);


    public static ReturnValue show(long hwnd, String message, String caption, Buttons buttons, Icon icon) throws Throwable {
        try (Arena arena = Arena.openConfined()) {
            MemorySegment messageMS = allocateUtf16String(arena, message);
            MemorySegment captionMS = allocateUtf16String(arena, caption);

            long style = buttons.bits | icon.bits;

            MemorySegment messageBoxWMS = SymbolLookup.loaderLookup().find("MessageBoxW").orElseThrow();
            MethodHandle messageBoxMH = Linker.nativeLinker().downcallHandle(messageBoxWMS, messageBoxWFD);

            long result = (long) messageBoxMH.invokeExact(hwnd, messageMS, captionMS, style);

            return Arrays.stream(ReturnValue.values()).filter(returnValue -> returnValue.bits == result).findFirst().orElseThrow();
        }
    }

    private static MemorySegment allocateUtf16String(Arena arena, String str) {
        Objects.requireNonNull(str);
        byte[] bytes = str.getBytes(StandardCharsets.UTF_16LE);

        // allocate 2 bytes more for terminating null
        MemorySegment offHeapSegment = arena.allocate(bytes.length + 2L);
        MemorySegment onHeapSegment = MemorySegment.ofArray(bytes);

        // copy bytes over off heap
        offHeapSegment.copyFrom(onHeapSegment);

        // set terminating 0 characters
        offHeapSegment.set(ValueLayout.JAVA_BYTE, bytes.length, (byte) 0x00);
        offHeapSegment.set(ValueLayout.JAVA_BYTE, bytes.length - 1L, (byte) 0x00);

        return offHeapSegment;
    }

    public enum Buttons {
        MB_ABORTRETRYIGNORE(0x00000002L), MB_CANCELTRYCONTINUE(0x00000006L), MB_HELP(0x00004000L), MB_OK(0x00000000L), MB_OKCANCEL(0x00000001L), MB_RETRYCANCEL(0x00000005L), MB_YESNO(0x00000004L), MB_YESNOCANCEL(0x00000003L);

        public final long bits;

        Buttons(long bits) {
            this.bits = bits;
        }
    }

    public enum Icon {
        MB_ICONEXCLAMATION(0x00000030L), MB_ICONWARNING(0x00000030L), MB_ICONINFORMATION(0x00000040L), MB_ICONASTERISK(0x00000040L), MB_ICONQUESTION(0x00000020L), MB_ICONSTOP(0x00000010L), MB_ICONERROR(0x00000010L), MB_ICONHAND(0x00000010L);

        public final long bits;

        Icon(long bits) {
            this.bits = bits;
        }
    }

    public enum ReturnValue {
        IDABORT(3), IDCANCEL(2), IDCONTINUE(11), IDIGNORE(5), IDNO(7), IDOK(1), IDRETRY(4), IDTRYAGAIN(10), IDYES(6);

        public final long bits;

        ReturnValue(long bits) {
            this.bits = bits;
        }
    }
}