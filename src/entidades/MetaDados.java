package entidades;

import java.io.Serializable;

public class MetaDados implements Serializable {
	private String coluna;
	private String tipo;
	private Integer tamanho;
	
	public MetaDados() {

	}
	
	public MetaDados(String coluna) {
		this.coluna = coluna;
	}
	
	
	
	public String getColuna() {
		return coluna;
	}
	public void setColuna(String coluna) {
		this.coluna = coluna;
	}
	public String getTipo() {
		return tipo;
	}
	public void setTipo(String tipo) {
		this.tipo = tipo;
	}
	public Integer getTamanho() {
		return tamanho;
	}
	public void setTamanho(Integer tamanho) {
		this.tamanho = tamanho;
	}
	
	@Override
	public int hashCode() {
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + ((coluna == null) ? 0 : coluna.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final MetaDados other = (MetaDados) obj;
		if (coluna == null) {
			if (other.coluna != null)
				return false;
		} else if (!coluna.equals(other.coluna))
			return false;
		return true;
	}
	
}
