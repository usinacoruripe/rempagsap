package negocio;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.InvalidPropertiesFormatException;

import org.apache.commons.io.IOUtils;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;

public class Run {
	public static void main(String[] args) throws InvalidPropertiesFormatException, FileNotFoundException, IOException {
		/*File arqProp = new File("properties.xml");
		Properties prop = new Properties();
		prop.loadFromXML(new FileInputStream(arqProp));
		
		System.out.println(prop.getProperty("sc-hora_inicio"));*/
		
		moverArquivo("//appdtc204/SeniorPRD$/temp/TesteInf.PDF", "arquivos/informe_rendimentos/informe_207608-1.pdf", "svc.informerend", "1nf0rm32018*");
		
	}
	
	public static boolean moverArquivo(String origem, String destino, String usuarioRede, String senhaRede ) throws IOException {		
		SmbFileInputStream in = null;
		FileOutputStream out = null;
		boolean sucesso = false;
		
		
		try {			
			NtlmPasswordAuthentication auth = new NtlmPasswordAuthentication("USINACORURIPE", usuarioRede, senhaRede);

			SmbFile arquivoOrigem = new SmbFile("smb:"+origem, auth);
			
	
			in = new SmbFileInputStream(arquivoOrigem);
			out = new FileOutputStream(destino);

			out.write(IOUtils.toByteArray(in));
			
			sucesso = true;						
			
		} finally {
			if(in != null) in.close();
			if(out != null) out.close();
		}   
		
		return sucesso;
	}
}
