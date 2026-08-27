package com.github.gcolin.event;

import java.io.IOException;
import java.nio.file.Paths;

public class PapyUploadManual {

	public static void main(String[] args) throws IOException, InterruptedException {
		PapiUlploadService service = new PapiUlploadService();
		
		service.upload("72491", "KDZDSYYGIJ", Paths.get("C:\\Users\\modul\\Downloads\\Rapide_de_Tr_gor__checs_Juin_2026.papi"));

	}

}
