
public class SquaredMatriceCLI {
	int length; 
	float values;
	public SquaredMatriceCLI(int length) {
		this.length=length;
		this.values=1;
	}
	
	public SquaredMatriceCLI(int length, float values) {
		this.length=length;
		this.values=(float)(1./length);
	}
	
	public SquaredMatriceCLI produit(float value) {
		this.values*=value;
		return this;
	}
	
}
