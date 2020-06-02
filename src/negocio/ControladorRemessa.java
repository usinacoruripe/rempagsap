package negocio;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.rmi.Remote;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Calendar;
import java.util.Date;
import java.util.InvalidPropertiesFormatException;
import java.util.Timer;
import java.util.TimerTask;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.time.DateFormatUtils;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileOutputStream;
import webcoruripe.apresentacao.MsgNotificacao;
import webcoruripe.negocio.Email;
import webcoruripe.negocio.Format;

public class ControladorRemessa extends TimerTask implements ServicoRemessaRemoto { 
	private Timer agendador;
	private static final int tamanhoMaximoLog = 4096000;
	public static final String BANCO_ITAU = "itau";
	public static final String BANCO_SANTANDER = "std";
	
	ControladorConfiguracao configuracao;

	public ControladorRemessa() {
		this.agendador = new Timer();
		this.configuracao = new ControladorConfiguracao();
	}

	@Override
	public void iniciarServico() throws Exception {
		Remote stub = (Remote) UnicastRemoteObject.exportObject(this, 0);
		Registry registry = LocateRegistry.createRegistry(configuracao.getScPort());
		registry.bind(this.configuracao.getScName(), stub);
		
		Date dataInicio = Format.stringToDate(Format.formatDate(new Date(), "dd/MM/yyyy") + " " + this.configuracao.getHoraInicioServico(), "dd/MM/yyyy HH:mm:ss");

		//Execura a cada 5 minutos (300000 milisegundos)
		agendador.schedule(this, dataInicio, new Long(this.configuracao.getIntervaloExecucaoMilisseguntos()));

	}

	@Override
	public void pararServico() throws Exception {
		this.agendador.cancel();

		UnicastRemoteObject.unexportObject(this, false);

	}

	@Override
	public void executarCopiaArquivosRemessa(String banco) throws Exception {
		FileWriter writer = null;
		PrintWriter saida = null;

		try {			
			verificarLimiteLog("registro");
			writer = new FileWriter(getArquivoLog("registro"),true);
			saida = new PrintWriter(writer,true);

			//saida.println("** Cópia iniciada em " + DateFormatUtils.format(new Date(), "dd/MM/yyyy HH:mm:ss") +"**");

			//Lista os arquivos	ITAU	
			File pastaOrigem = new File(this.configuracao.getCaminhoOrigem(banco));
			File[] arquivos = pastaOrigem.listFiles();
			String arquivosCopiados = "";
			String arquivosNaoCopiados = "";
			StringBuffer msgErro = null;
			StringBuffer nomeArquivoDestino = null;
			
			for (File file : arquivos) {
				if( file.isFile() && file.getName().startsWith("REMPAG") ) {
					msgErro = new StringBuffer();
					nomeArquivoDestino = new StringBuffer();
					nomeArquivoDestino.append(this.configuracao.getCaminhoDestino(banco)+File.separator+file.getName());
					
					if( this.moverArquivo(file, nomeArquivoDestino, this.configuracao.getUsuarioRede(), this.configuracao.getSenhaRede(), msgErro) ) {
						saida.println("Arquivo copiado: " + nomeArquivoDestino);
						arquivosCopiados += ("<br>" + nomeArquivoDestino);
						file.delete();
					} else {
						saida.println("Erro ao copiar arquivo: " + nomeArquivoDestino+". " + ( msgErro.length() > 0 ? msgErro.toString() : "" ));
						arquivosNaoCopiados += ("<br>" + nomeArquivoDestino + ( msgErro.length() > 0 ? ". " + msgErro.toString() : "" ));
					}
				}
			}
			
			this.enviarEmailCopias(arquivosCopiados, arquivosNaoCopiados);

			saida.println("** BANCO "+ banco +": Cópia finalizada em " + DateFormatUtils.format(new Date(), "dd/MM/yyyy HH:mm:ss") +"**");
			saida.println(" ");			


		} catch (Throwable e) {
			verificarLimiteLog("erro");
			try {
				writer = new FileWriter(getArquivoLog("erro"),true);
				saida = new PrintWriter(writer,true);
				saida.append("["+DateFormatUtils.format(new Date(), "dd/MM/yyyy HH:mm:ss")+"]");
				saida.append("\n");
				e.printStackTrace(saida);
				this.enviarEmailErro(e);
				saida.close();  
				writer.close();
			} catch (Exception e2) {
				e2.printStackTrace();
			}
		} finally {
			if( saida  != null ) saida.close();  
			if( writer != null ) writer.close();
		}

	}	

	public static void main(String[] args) throws Exception {
		try{
			String acao = args == null || args.length == 0 ? "start" : args[0];
			if( acao.equals("executar") ) {
				new ControladorRemessa().run();
			}
			
			if (acao.equals("start")) {
				new ControladorRemessa().iniciarServico();

				verificarLimiteLog("registro");
				FileWriter writer = new FileWriter(getArquivoLog("registro"),true);
				PrintWriter saida = new PrintWriter(writer,true);
				saida.println("Servico iniciado em " + DateFormatUtils.format(new Date(), "dd/MM/yyyy HH:mm:ss"));				
				saida.close();				
			}
			ControladorConfiguracao configuracao = new ControladorConfiguracao();
			if (acao.equals("stop")) {
				Registry registry = LocateRegistry.getRegistry("localhost", configuracao.getScPort());
				ServicoRemessaRemoto servicoRemoto = (ServicoRemessaRemoto) registry.lookup(configuracao.getScName());				
				servicoRemoto.pararServico();

				verificarLimiteLog("registro");
				FileWriter writer = new FileWriter(getArquivoLog("registro"),true);
				PrintWriter saida = new PrintWriter(writer,true);
				saida.println("Servico Parado em " + DateFormatUtils.format(new Date(), "dd/MM/yyyy HH:mm:ss"));				
				saida.close();

			}
			//==== Linha abaixo usada para executar testes
			//new ControladorRemessa().executarCopiaArquivosRemessa();

		} catch (Throwable e) {
			verificarLimiteLog("erro");
			try {
				FileWriter writer = new FileWriter(getArquivoLog("erro"),true);
				PrintWriter saida = new PrintWriter(writer,true);
				saida.append("["+DateFormatUtils.format(new Date(), "dd/MM/yyyy HH:mm:ss")+"]");
				e.printStackTrace(saida);
				new ControladorRemessa().enviarEmailErro(e);
				saida.close();  
				writer.close();
			} catch (Exception e2) {}
		}
	}

	private void enviarEmailErro(Throwable e) throws AddressException, MalformedURLException, InvalidPropertiesFormatException, FileNotFoundException, MessagingException, IOException {
		ControladorConfiguracao configuracao = new ControladorConfiguracao();

		MsgNotificacao msg = new MsgNotificacao("Erro no Controlador de copias - Arquivo de remessa");
		msg.setCorpo("Foi gerado um erro durante a cópia dos arquivos de remessa. Favor verificar a descrição do erro abaixo.<BR><BR>" + e.getMessage());

		String destinatarios[] = configuracao.getMailResponsaveis().split(";");

		for (String dest : destinatarios) {
			Email.sendMail(configuracao.getMailHost(),
					"Erro no Controlador de copias - Arquivo Remessa", dest, "webcoruripe@usinacoruripe.com.br", msg.toString());
		}

	}

	private void enviarEmailCopias(String arquivosCopiados, String arquivosNaoCopiados) throws AddressException, MalformedURLException, InvalidPropertiesFormatException, FileNotFoundException, MessagingException, IOException {
		ControladorConfiguracao configuracao = new ControladorConfiguracao();
		
		if( ( arquivosCopiados == null || arquivosCopiados.trim().equals("") )
				&& ( arquivosNaoCopiados == null || arquivosNaoCopiados.trim().equals("") )  )
			return;
		
		String corpo = "O controle de cópias foi executado e retornou o reguinte resultado:";
		if( arquivosCopiados != null && !arquivosCopiados.trim().equals("") ) {
			corpo += "<br><br>";
			corpo += "Os arquivos abaixo foram copiados:";
			corpo += arquivosCopiados;
		}

		if( arquivosNaoCopiados != null && !arquivosNaoCopiados.trim().equals("") ) {
			corpo += "<br><br>";
			corpo += "Não foi possível copiar os seguintes arquivos:";
			corpo += arquivosNaoCopiados;
		}

		MsgNotificacao msg = new MsgNotificacao("Controle de cópias - Arquivos de remessa");
		msg.setCorpo( corpo );

		String destinatarios[] = configuracao.getMailFinanceiro().split(";");

		for (String dest : destinatarios) {
			Email.sendMail(configuracao.getMailHost(),
					"Controle de copias - Arquivo Remessa", dest, "webcoruripe@usinacoruripe.com.br", msg.toString());
		}

	}

	public static File getArquivoLog(String arquivo) {
		File diretorio = new File("logs");
		if (!diretorio.exists())
			diretorio.mkdir();		
		return new File("logs" + File.separator + arquivo + ".log");
	}

	public static void verificarLimiteLog(String nomeArquivoLog) {
		File file = getArquivoLog(nomeArquivoLog);
		if (file.length() > tamanhoMaximoLog) {
			Calendar c = Calendar.getInstance();
			file.renameTo(getArquivoLog(nomeArquivoLog+"." + c.get(Calendar.DAY_OF_MONTH) + "-" + Format.formatNumber(c.get(Calendar.MONTH),"00") + "-" + c.get(Calendar.YEAR)));
		}
	}

	private boolean moverArquivo(File origem, StringBuffer destino, String usuarioRede, String senhaRede, StringBuffer msgErro ) throws IOException {		
		SmbFileOutputStream out = null;
		FileInputStream fis = null;
		boolean sucesso = false;
		int cont = 1;
		String dataHora = Format.formatDate(new Date(), "yyMMdd") + Format.formatDate(new Date(), "HHmm");
		String nomeArquivoDestino = destino.toString();
		//remove sequencial do SAP. Ex.: /usr/sap/DMS/Financeiro/REMPAG01 -> /usr/sap/DMS/Financeiro/REMPAG
		nomeArquivoDestino = nomeArquivoDestino.substring(0,nomeArquivoDestino.length()-2) + dataHora;
		
		try {			
			NtlmPasswordAuthentication auth = new NtlmPasswordAuthentication("USINACORURIPE", usuarioRede, senhaRede);

			SmbFile arquivoDestino = new SmbFile("smb:"+nomeArquivoDestino, auth);
			
			while( arquivoDestino.exists() ) {
				cont++;
				arquivoDestino = new SmbFile("smb:"+nomeArquivoDestino + Format.formatNumber(cont, "00"), auth);
			}
			
			out = new SmbFileOutputStream(arquivoDestino);
			fis = new FileInputStream(origem);

			out.write(IOUtils.toByteArray(fis));
			
			sucesso = true;
			
			destino.delete(0, destino.length());
			destino.append(arquivoDestino.getName());
						
			
		} catch (Throwable e) {
			verificarLimiteLog("erro");
			FileWriter writer = null;
			PrintWriter saida = null;
			try {
				writer = new FileWriter(getArquivoLog("erro"),true);
				saida = new PrintWriter(writer,true);
				saida.append("["+DateFormatUtils.format(new Date(), "dd/MM/yyyy HH:mm:ss")+"]");
				saida.append("\n");
				
				saida.append("Origem: " + origem.getAbsolutePath() + "\n");
				saida.append("Destino: " + destino + "\n");
				
				e.printStackTrace(saida);
				this.enviarEmailErro(e);
				saida.close();  
				writer.close();
			} catch (Exception e2) {
				e2.printStackTrace();
			}
		} finally {
			if(fis != null) fis.close();
			if(out != null) out.close();
		}   
		
		return sucesso;
	}

	@Override
	public void run() {
		try {
			//Processa banco Itau
			this.executarCopiaArquivosRemessa(ControladorRemessa.BANCO_ITAU);
			
			//Processa banco Santander
			this.executarCopiaArquivosRemessa(ControladorRemessa.BANCO_SANTANDER);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
