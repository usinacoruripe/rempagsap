package negocio;

import java.rmi.Remote;

public interface ServicoRemessaRemoto extends Remote {
	public void iniciarServico() throws Exception;
	
	public void pararServico() throws Exception;
	
	public void executarCopiaArquivosRemessa(String banco) throws Exception;
}
