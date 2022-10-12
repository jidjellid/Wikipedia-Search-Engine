
public class MatriceCLI {

	int posCI;
	int posL;

	float	[] C;
	int 	[] L;
	int 	[] I;
	
	float empty;
	float old0;

	public MatriceCLI(int c, int l, int i) {
		System.out.println("Initializing matrix of size :"+(l-1));
		this.C = new float[c];
		this.L = new int[l];
		this.I = new int[i];
		this.empty=1f/(l-1);
		this.old0 =0f;
		
		posCI = 0;
		posL = 0;
		L[posL] = 0;
	}

	public MatriceCLI(float [] c, int [] l, int [] i) {
		this.C=c;
		this.L=l;
		this.I=i;
		this.empty=1f/(l.length-1);
		this.old0 = 0f;
		
		posCI = c.length;
		posL = l.length;
	}
	
	public static MatriceCLI createMatriceCLI(float[][] matrice) {
		float	[] c = new float[countElements(matrice)];
		int 	[] l = new int	[matrice.length+1];
		int 	[] i = new int	[c.length];
		int c_index = 0;
		
		int l_start	= 0;
		int l_end	= 1;
		
		int i_start	= 0;
		int i_end	= 0;
		
		for(int row=0; row<matrice.length; row++) {
			for(int col=0; col<matrice[row].length; col++) {
				if(matrice[row][col]!=0) {
					c[c_index]=matrice[row][col];
					c_index++;
					
					i[i_end]=col;
					i_end++;
				}
			}
			if(i_start!=i_end) {
				for(int start = l_start; start<l_end; start++) {
					l[start] = i_start;
				}
				l_start=l_end;
			}
			l_end++;
			i_start=i_end;
		}
		l[l.length-1]=c.length;
		return new MatriceCLI(c,l,i);
	}
	
	
	private static int countElements(float[][] matrice) {
		int nb = 0;
		for(int i=0; i<matrice.length; i++) {
			for(int j=0; j<matrice[i].length; j++) {
				if(matrice[i][j]!=0) {
					nb++;
				}
			}
		}return nb;
	}
	
	
	public float [] produit(float [] vector) {
		if(vector.length<L.length-1) {
			System.out.println("Unexpected behavior, vector should have the same row length than matrice");
		}
		float [] res = new float[vector.length];
		int c_index = 0;
		for(int row=0; (row<L.length-1 && row<vector.length); row++) {
			int start 	= L[row];
			int end		= L[row+1];
			for(int i = 0; i<(end-start); i++, c_index++) {
				res[row] += (C[c_index] * vector[I[c_index]]);
			}
		}
		return res;
	}
	
	public MatriceCLI produit(float k) {
		for(int i=0; i<this.C.length; i++) {
			C[i]*=k;
		}
		this.empty*=k;
		this.old0 *=k;
		return this;
	}
	
	
	
	public float [] transpose_produit(float [] vector) {
		if(vector.length<L.length-1) {
			System.out.println("Unexpected behavior, vector should have the same row length than matrice");
		}
		float [] res = new float[vector.length];
		int c_index = 0;
		for(int row=0; (row<L.length-1 && row<vector.length); row++) {
			int start 	= L[row];
			int end		= L[row+1];
			if(start==end) {
				for(int i=0; i<vector.length; i++) {
					res[i] += (this.empty * vector[row] );
				}
			}else {
				for(int i = 0; i<vector.length; i++) {
					if(I[c_index]==i) {
						res[I[c_index]] += (C[c_index] * vector[row]);
						c_index++;
					}else {
						res[i] += (this.old0 * vector[row]);
					}
				}
			}
		}
		return res;
	}
	
	
	public float [] transpose_produit(float [] vector, float epsilon) {
		if(vector.length<L.length-1) {
			System.out.println("Unexpected behavior, vector should have the same row length than matrice");
		}
		float [] res = new float[vector.length];
		int c_index = 0;
		int size = L.length-1;
		boolean [] check;
		for(int row=0; row<vector.length; row++) {
			int start 	= L[row];
			int end		= L[row+1];
			if(start==end) {
				for(int i=0; i<vector.length; i++) {
					res[i] += (this.empty * vector[row] );
				}
			}else {
				check = new boolean[vector.length];
				for(int i = 0; i<(end-start); i++, c_index++) {
					res[I[c_index]] += (C[c_index] * vector[row]);
					check[I[c_index]]=true;
				}
				for(int i=0; i<check.length; i++) {
					if(!check[i]) {
						res[i] += (this.old0 * vector[row]);
					}
				}
			}
		}
		
		//Je ne sais pas si cette ligne sert à quelquechose ...
		for(int i=0; i<vector.length; i++) {
			res[i]=((1-epsilon)*res[i]) + epsilon/size;
		}
		//Avec 0.3501166 0.27668867 0.15020902 0.22298616 sum = 1.0000005
		//Sans 0.36822265 0.28363097 0.12713635 0.22101115 sum = 1.0000011
		return res;
	}
	
	public MatriceCLI add(SquaredMatriceCLI M) {	
		for(int i=0; i<C.length; i++) {
			C[i]+=M.values;
		}
		this.empty+=M.values;
		this.old0 +=M.values;
		return this;
	}
	
	//L : 0 3 5 5 6
	//C : 3 5 8 1 2 3
	//I : 1 2 3 0 2 1
	
	/*
	 *  0 3 5 8
	 *  1 0 2 0
	 *  0 0 0 0
	 *  0 3 0 0
	 */
	
	//L : 0 3 5 9 10
	//C : 3 5 8 1 2 0 0 0 0 3
	//I : 1 2 3 0 2 0 1 2 3 1
	
	/*	  
	 * 	C : 0.4625 0.4625 0.88750005 0.32083336 0.32083336 0.32083336 
		L : 0 2 3 3 6 
		I : 1 3 0 0 1 2 
	 * 
	 *    null 0.46 null 0.46 
	 *    0.88 null null null
	 *    none none none none
	 *    0.32 0.32 0.32 null
	 *    
	 */

	public static MatriceCLI matrice_AG(MatriceCLI A, SquaredMatriceCLI J, float epsilon) {
		return A.produit( (1-epsilon) ).add( J.produit(epsilon/(A.L.length-1)) );
	}
	
	
	
	public float[] pageRank(MatriceCLI AG, SquaredMatriceCLI J, float [] distribution, int k, float epsilon) {
		float [] pi = new float[distribution.length];
		System.arraycopy(distribution, 0, pi, 0, pi.length);
		for(int i=0; i<k; i++) {
			pi = AG.transpose_produit(pi, epsilon);
		}
		return pi;
	}
	
	
	public void print() {
		System.out.print("C : ");
		for(float c : C) {
			System.out.print(c+" ");
		}
		System.out.print("\nL : ");
		for(int c : L) {
			System.out.print(c+" ");
		}
		System.out.print("\nI : ");
		for(int c : I) {
			System.out.print(c+" ");
		}
		System.out.println();
	}

	public void asMatrix(){

		int pos = 0;
		int currentPrintsForLines = 0;

		for(int l = 1; l < L.length; l++) {//Line
			int nbValuesInLine = L[l] - L[l-1];
			
			for(int y = 0; y < L.length-1; y++) {//Col
				if(pos == C.length || nbValuesInLine == 0 || nbValuesInLine == currentPrintsForLines){
					System.out.print("0 ");	
				} else {
					boolean shouldPrint0 = true;

					for(int x = 0; x < nbValuesInLine-currentPrintsForLines; x++) {
						if(y == I[pos+x]){//If current col == col in the I
							System.out.print(C[pos+x]+" ");
							currentPrintsForLines++;
							shouldPrint0 = false;
							break;
						}
					}

					if(shouldPrint0)
						System.out.print("0 ");	
				}
			}
	
			pos += nbValuesInLine;
			currentPrintsForLines = 0;
			System.out.println();
		}
	}
}