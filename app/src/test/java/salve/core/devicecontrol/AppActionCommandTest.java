package salve.core.devicecontrol;

import static org.junit.Assert.*;
import static salve.core.devicecontrol.AppActionCommand.Type.*;

import org.junit.Test;

public class AppActionCommandTest {
    private static void expect(String input, AppActionCommand.Type type, String target) {
        AppActionCommand command = AppActionCommand.parse(input);
        assertNotNull(input, command);
        assertEquals(input, type, command.type);
        assertEquals(input, target, command.target);
    }

    @Test public void navigationRequestsKeepTheDestination() {
        expect("Llévame a la Plaza de la Virgen Blanca", NAVIGATE, "la Plaza de la Virgen Blanca");
        expect("salve, llévame al trabajo", NAVIGATE, "trabajo");
        expect("¿Cómo llego a Bilbao?", NAVIGATE, "Bilbao");
        expect("pon ruta a Calle Dato 12, Vitoria", NAVIGATE, "Calle Dato 12, Vitoria");
        expect("abre maps hacia el aeropuerto", NAVIGATE, "el aeropuerto");
    }

    @Test public void musicNeedsAMusicCue() {
        expect("pon música de Rosalía", MUSIC, "Rosalía");
        expect("Ponme la canción Despechá en Spotify", MUSIC, "Despechá");
        expect("pon Bad Bunny en spotify", MUSIC, "Bad Bunny");
        expect("reproduce los éxitos de los 80", MUSIC, "éxitos de los 80");
        expect("quiero escuchar jazz tranquilo", MUSIC, "jazz tranquilo");
        assertNull(AppActionCommand.parse("pon una alarma a las 7"));
        assertNull(AppActionCommand.parse("pon la luz"));
    }

    @Test public void spotifyPlaylistsLikedSongsAndSearch() {
        expect("pon mi lista Gym", PLAYLIST, "Gym");
        expect("Salve, ponme la playlist Para correr en Spotify", PLAYLIST, "Para correr");
        expect("reproduce mi lista de reproducción Relax", PLAYLIST, "Relax");
        expect("pon mi lista de música que se llama Verano 2026", PLAYLIST, "Verano 2026");
        expect("pon una de mis listas llamada Coche", PLAYLIST, "Coche");
        expect("pon mis me gusta", LIKED, "mis me gusta");
        expect("ponme las canciones que me gustan en Spotify", LIKED, "las canciones que me gustan");
        expect("busca Rosalía en Spotify", MUSIC_SEARCH, "Rosalía");
        // Las listas que no son de música no se tocan.
        assertNull(AppActionCommand.parse("pon la lista de la compra"));
        // «abre …» sigue siendo «abrir app» (solo actúa si hay una app con ese nombre), nunca música.
        assertEquals(OPEN_APP, AppActionCommand.parse("abre mi lista de tareas").type);
        // Lo que ya funcionaba sigue igual.
        expect("pon Bad Bunny en spotify", MUSIC, "Bad Bunny");
    }

    @Test public void callsCarryANameOrNumberAndNothingElse() {
        expect("Llama a mamá", CALL, "mamá");
        expect("llámale a Ana García con altavoz", CALL, "Ana García");
        expect("llama a +34 600 123 456", CALL, "+34 600 123 456");
        assertTrue(AppActionCommand.isPhoneNumber("+34 600 123 456"));
        assertFalse(AppActionCommand.isPhoneNumber("Ana"));
        assertNull(AppActionCommand.parse("llama"));
        assertNull(AppActionCommand.parse("llama a " + "x".repeat(80)));
    }

    @Test public void openAppKeepsJustTheAppName() {
        expect("abre la cámara", OPEN_APP, "la cámara");
        expect("Salve, abre WhatsApp", OPEN_APP, "WhatsApp");
        expect("abre la aplicación de ajustes", OPEN_APP, "ajustes");
        expect("lanza Spotify", OPEN_APP, "Spotify");
        expect("abre maps hacia el aeropuerto", NAVIGATE, "el aeropuerto");
        assertNull(AppActionCommand.parse("abre WhatsApp y escribe a Ana"));
        assertNull(AppActionCommand.parse("abre la puerta que da al jardín de atrás"));
    }

    @Test public void ordinaryTalkAndMultilineTextAreIgnored() {
        assertNull(AppActionCommand.parse("¿qué tiempo hace?"));
        assertNull(AppActionCommand.parse("vamos"));
        assertNull(AppActionCommand.parse("llama a Ana\ny dile algo"));
        assertNull(AppActionCommand.parse(null));
    }

    @Test public void contactSearchTreatsWildcardsLiterally() {
        assertEquals("%a\\%b\\_c%", AppActionCommand.likeArgument("a%b_c"));
    }
}
