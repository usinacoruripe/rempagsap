package negocio;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;

public class ControladorConfiguracao {
	private static Properties prop;
	private static long ultmaModificacao;
	
	private Properties carregarPropriedades() throws InvalidPropertiesFormatException, FileNotFoundException, IOException {
		File arqProp = new File("properties.xml");
		if (prop == null || arqProp.lastModified() > ultmaModificacao) {
			prop = new Properties();
			prop.loadFromXML(new FileInputStream(arqProp));
			ultmaModificacao = arqProp.lastModified();
		}
		return prop;
	}
	
	public String getCaminhoOrigem(String banco) throws InvalidPropertiesFormatException, FileNotFoundException, IOException {
		return this.carregarPropriedades().getProperty("caminho-origem-"+banco);
	}
	
	public String getCaminhoDestino(String banco) throws InvalidPropertiesFormatException, FileNotFoundException, IOException {
		return this.carregarPropriedades().getProperty("caminho-destino-"+banco);
	}
	
	public String getUsuarioRede() throws InvalidPropertiesFormatException, FileNotFoundException, IOException {
		return this.carregarPropriedades().getProperty("usuario-rede");
	}
	
	public String getSenhaRede() throws InvalidPropertiesFormatException, FileNotFoundException, IOException {
		return this.carregarPropriedades().getProperty("senha-rede");
	}
	
	public String getMailHost() throws InvalidPropertiesFormatException, FileNotFoundException, IOException {
		return this.carregarPropriedades().getProperty("mail-host");
	}
	
	public String getMailResponsaveis() throws InvalidPropertiesFormatException, FileNotFoundException, IOException {
		return this.carregarPropriedades().getProperty("mail-responsaveis");
	}
	
	public String getMailFinanceiro() throws InvalidPropertiesFormatException, FileNotFoundException, IOException {
		return this.carregarPropriedades().getProperty("mail-financeiro");
	}
	
	public Integer getScPort() throws InvalidPropertiesFormatException, FileNotFoundException, IOException {
		return new Integer(this.carregarPropriedades().getProperty("sc-port"));
	}	

	public String getScName() throws InvalidPropertiesFormatException, FileNotFoundException, IOException {
		return this.carregarPropriedades().getProperty("sc-name");
	}
	
	public String getHoraInicioServico() throws InvalidPropertiesFormatException, FileNotFoundException, IOException {
		return this.carregarPropriedades().getProperty("hora-inicio");
	}
	
	public String getIntervaloExecucaoMilisseguntos() throws InvalidPropertiesFormatException, FileNotFoundException, IOException {
		return this.carregarPropriedades().getProperty("intervalo-milissegundos");
	}	
}
