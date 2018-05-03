import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Stack;

public class FileSystemUtility {

	public static void main(String[] args) throws IOException {
		
		long BPB_BytesPerSec;
		long BPB_SecPerClus;
		long BPB_RsvdSecCnt;
		long BPB_NumFATS;
		long BPB_FATSz32;
		long pwdAddress;
		boolean running = true;
		Stack<Integer> stack = new Stack<Integer>();
		
		String path = "";
		String fileName = "";
		if(args.length > 0) {
			path = args[0].substring(0, args[0].lastIndexOf('\\'));
			fileName = args[0].substring(args[0].lastIndexOf('\\'));
		} else {System.out.println("No File System Provided");}
		Path diskPath = Paths.get(path, fileName);
		//Path diskPath = Paths.get("C:\\Users\\Avi\\Desktop\\OS-project-3\\OSHW3\\", "fat32.img");
		byte[] diskImage = Files.readAllBytes(diskPath);
		
		BPB_BytesPerSec = 0xFFFFL & ((diskImage[12] << 8) ^ diskImage[11]);
		BPB_SecPerClus = 0xFFL & diskImage[13];
		BPB_RsvdSecCnt = 0xFFFFL & ((diskImage[15] << 8) ^ diskImage[14]);
		BPB_NumFATS = 0xFFL & diskImage[16];
		long temp1 = 0xFFFFFFFFL & (diskImage[39] << 24);
		long temp2 = 0xFFFFFFL & (diskImage[38] << 16);
		long temp3 = 0xFFFFL & (diskImage[37] << 8);
		long temp4 = 0xFFL & (diskImage[36]);
		BPB_FATSz32 = temp1 + temp2 + temp3 + temp4;
		
		long FirstDataSector = BPB_RsvdSecCnt + BPB_NumFATS * BPB_FATSz32;
		 
		int N = 2;
		long FirstSectorOfCluster = (((N - 2) * BPB_SecPerClus) + FirstDataSector) * BPB_BytesPerSec;
		pwdAddress = FirstSectorOfCluster;
		int beginningOfData = (int) (FirstSectorOfCluster - (2 *512));
		String pwd = "root\\";
		long fatTable = BPB_RsvdSecCnt * 512;
		stack.push((int)pwdAddress);
		System.out.println(getNumberOfFreeClusters(diskImage, fatTable, beginningOfData));
		System.out.println(getFreeClusters(diskImage, fatTable, beginningOfData, 3));

		while(running) {
			System.out.print(pwd + "> ");
			Scanner scan = new Scanner(System.in);
			String input = scan.nextLine();
			String[] arr = input.split(" ");
			String command = arr[0];
			
			if(command.equals("info")) {
				info(BPB_BytesPerSec, BPB_SecPerClus, BPB_RsvdSecCnt, BPB_NumFATS, BPB_FATSz32);
			} else if(command.equals("stat")) {
				String file = arr[1];
				stat(diskImage, pwdAddress,fatTable, beginningOfData,file);
			} else if (command.equals("ls")) {
				if(arr.length > 1){
					if(arr[1].equals("..")) {
						if(stack.size() == 1) {
							System.out.println("Error: already in root directory");
							
						} else {
							Integer temp = stack.pop();
							Integer tempPwdAddress = stack.peek();
							ls(diskImage, (long)tempPwdAddress, fatTable, (long)beginningOfData, FirstSectorOfCluster);
						
							stack.push(temp);
						}
					}
					else {
						String file = arr[1];
						long lsAddress = cd(diskImage, pwdAddress, fatTable,beginningOfData, file);
						ls(diskImage, lsAddress, fatTable, beginningOfData, FirstSectorOfCluster);
					}
				}
				else{
					ls(diskImage, pwdAddress, fatTable, beginningOfData, FirstSectorOfCluster);
				}
			} else if(command.equals("exit")) {
				running = false;
			} else if(command.equals("cd")) {
				String file = arr[1];
				if(file.equals("..")){
					if(stack.size() == 1) {
						System.out.println("Error: already in root directory");
					} 
					else {
						stack.pop();
						pwdAddress = stack.peek();
						pwd = pwd.substring(0, pwd.lastIndexOf('\\'));
						pwd = pwd.substring(0, pwd.lastIndexOf('\\') + 1);
					}
				}
				else {
					long temp = pwdAddress;
					pwdAddress = cd(diskImage, pwdAddress, fatTable,beginningOfData, file);
					if(temp != pwdAddress) {
						pwd += file + "\\";
						stack.push((int) pwdAddress);
					}
					//stack.push((int) pwdAddress);
				}
			} else if(command.equals("read")) {
				String file = arr[1];
				int offset = Integer.parseInt(arr[2]);
				int amount = Integer.parseInt(arr[3]);
				read(diskImage, pwdAddress, offset, amount, fatTable, beginningOfData,file);
			} else if(command.equals("volume")) {
				volume(diskImage);
			}
			
		}
		
	}
	
	public static void info(long bytesPerSec, long secPerClus, long rsvdSecCnt, long numFATS, long fatsz32) {
		System.out.println("BPB_BytesPerSec is " + bytesPerSec);
		System.out.println("BPB_SecPerClus is " + secPerClus);
		System.out.println("BPB_RsvdSecClus is " + rsvdSecCnt);
		System.out.println("BPB_NumFATS is " + numFATS);
		System.out.println("BPB_FATSz32 is " + fatsz32);	
	}
	
	public static void printRootDirectory(byte[] disk, long root) {
		StringBuilder str = new StringBuilder();
		for(int i = 0; i < 11; i++) {
			str.append((char)disk[(int)root + i]);
		}
		for(int i = 0; i < 11; i++){
			System.out.println(str);
			root += 32;
			for(int j = 0; j < 11; j++) {
				str.setCharAt(j, (char)disk[(int)root + j]);
			}
		}	
	}
	
	public static void stat(byte[] disk, long root, long fatAddress, long rootAddress, String entry) {
		long clusterHigh = 0xFFff & ((disk[(int)root + 21] << 8) ^ (disk[(int)root + 20]));
		long clusterLow  = 0xFFFF & ((disk[(int)root + 27] << 8) ^ (disk[(int)root + 26]));
		long clusterNumber = 0xFFFFFFFF &((clusterHigh << 16) ^ clusterLow); 
		System.out.println("Entry: " + entry);
		StringBuilder str = new StringBuilder();
		for(int i = 0; i < 11; i++) {
			str.append((char)disk[(int)root + i]);
		}
		boolean foundEntry = false;
		do{
			for(int j = 0; j < 11; j++) {
				str.setCharAt(j, (char)disk[(int)root + j]);
			}
			String word = str.toString();
			word = word.replaceAll("\\s+","");
			String entryNew = entry.replace(".","");
			if(word.equals(entryNew)){
				foundEntry = true;
				//System.out.println("we found " + entry);
				//Just going to do it this way and we can figure out why this large shifting and masking isn't working
				long temp1 = 0xFFFFFFFF & (disk[(int)root + 31] << 24);
				long temp2 = 0xFFFFFF & (disk[(int)root + 30] << 16);
				long temp3 = 0xFFFF & (disk[(int)root + 29] << 8);
				long temp4 = 0xFF & (disk[(int)root + 28]);
				long size = temp1 ^ temp2 ^ temp3 ^ temp4;
				
				//Now the attributes
				StringBuilder str2 = new StringBuilder();
				byte attribute_byte = disk[(int)root + 11];
				if((attribute_byte & 0x01) != 0) {
					str2.append("ATTR_READ_ONLY ");
				}
				if((attribute_byte & 0x02) != 0) {
					str2.append("ATTR_HIDDEN ");
				}
				if((attribute_byte & 0x04) != 0) {
					str2.append("ATTR_SYSTEM ");
				}
				if((attribute_byte & 0x08) != 0) {
					str2.append("ATTR_VOLUME_ID ");
				}
				if((attribute_byte & 0x10) != 0) {
					str2.append("ATTR_DIRECTORY ");
				}
				if((attribute_byte & 0x20) != 0) {
					str2.append("ATTR_ARCHIVE");
				}
				
				//Now next cluster number. bytes 27, 26. String.format formats to keep the print in hex.
				StringBuilder str3 = new StringBuilder();
				str3.append(String.format("%02X", disk[(int)root + 27]));
				str3.append(String.format("%02X", disk[(int)root + 26]));
				
				System.out.println("Size is " + size);
				System.out.println("Attributes: " + str2.toString());
				System.out.println("Next cluster number is: " + str3.toString());
				
			}
			
			root += 32;
			if (root % 512 == 0) {
				int fatIndex = (int)fatAddress + (int)(clusterNumber * 4);
				long temp1 = 0xFFFFFFFF & (disk[(int)fatIndex +3] << 24);
				long temp2 = 0xFFFFFF & (disk[(int)fatIndex + 2] << 16);
				long temp3 = 0xFFFF & (disk[(int)fatIndex + 1] << 8);
				long temp4 = 0xFF & (disk[(int)fatIndex + 0]);
				clusterNumber = temp1 ^ temp2 ^ temp3 ^ temp4;
				root = (int)rootAddress + (int)clusterNumber*512;
			}
		}while(str.toString().trim().length() > 0);	
		if(!foundEntry){
			System.out.println("Could not find " + entry);
		}
	}
	
	public static void ls(byte[] disk, long pwd, long fatAddress, long rootAddress, long FirstSectorOfCluster) {
		long clusterHigh = 0xFFff & ((disk[(int)pwd + 21] << 8) ^ (disk[(int)pwd + 20]));
		long clusterLow  = 0xFFFF & ((disk[(int)pwd + 27] << 8) ^ (disk[(int)pwd + 26]));
		long clusterNumber = 0xFFFFFFFF &((clusterHigh << 16) ^ clusterLow); 
		if(pwd == FirstSectorOfCluster) {
			pwd += 32;
		}
		StringBuilder str = new StringBuilder();
		for(int i = 0; i < 11; i++) {
			str.append((char)disk[(int)pwd + + 32 + i]);
		}
		while(str.toString().trim().length() > 0){
			if(!str.toString().substring(8,9).equals(" ")){
				str.insert(8, '.');
			}
			else{ 
				str.insert(8, " ");
			}
			String fileName = str.toString().replaceAll(" ", "");
			System.out.println(fileName);
			str.deleteCharAt(8);
			pwd += 64;
			if (pwd % 512 == 0) {
				
				
				int fatIndex = (int)fatAddress + (int)(clusterNumber * 4);
				long temp1 = 0xFFFFFFFF & (disk[(int)fatIndex +3] << 24);
				long temp2 = 0xFFFFFF & (disk[(int)fatIndex + 2] << 16);
				long temp3 = 0xFFFF & (disk[(int)fatIndex + 1] << 8);
				long temp4 = 0xFF & (disk[(int)fatIndex + 0]);
				clusterNumber = temp1 ^ temp2 ^ temp3 ^ temp4;
				pwd = (int)rootAddress + (int)clusterNumber*512;
			}
			for(int j = 0; j < 11; j++) {
				str.setCharAt(j, (char)disk[(int)pwd + 32 + j]);
			}
		}	
	
	}
	
	public static long cd(byte[] disk, long pwd, long fatAddress, long rootAddress, String directoryName) {
		long clusterHigh = 0xFFff & ((disk[(int)pwd + 21] << 8) ^ (disk[(int)pwd + 20]));
		long clusterLow  = 0xFFFF & ((disk[(int)pwd + 27] << 8) ^ (disk[(int)pwd + 26]));
		long clusterNumber = 0xFFFFFFFF &((clusterHigh << 16) ^ clusterLow); 
		
		long pwdInit = pwd;
		StringBuilder str = new StringBuilder();
		boolean found = false;
		for(int i = 0; i < 11; i++) {
			str.append((char)disk[(int)pwd + i]);
		}
		if(directoryName.toString().trim().equals(".")){
			return pwdInit;
		}
		do {
			for(int j = 0; j < 11; j++) {
				str.setCharAt(j, (char)disk[(int)pwd + j]);
			}
			if(!str.toString().substring(8,9).equals(" ")){
				str.insert(8, '.');
			}
			else{ 
				str.insert(8, " ");
			}
			String fileName = str.toString().replaceAll(" ", "");
			//long clusterNumber = 0; 
			if(fileName.equals(directoryName)) {
				found = true;
				byte attribute_byte = disk[(int)pwd + 11];
				if((attribute_byte & 0x10) != 0) {
					clusterHigh = 0xFFff & ((disk[(int)pwd + 21] << 8) ^ (disk[(int)pwd + 20]));
					clusterLow  = 0xFFFF & ((disk[(int)pwd + 27] << 8) ^ (disk[(int)pwd + 26]));
					clusterNumber = 0xFFFFFFFF &((clusterHigh << 16) ^ clusterLow); 					
					return rootAddress + (512 * clusterNumber);
					
					
				} else {
					System.out.println("Cannot cd into a file");
					return pwdInit ;
				}
			}
			str.deleteCharAt(8);
			pwd += 32;
			if (pwd % 512 == 0) {
				int fatIndex = (int)fatAddress + (int)(clusterNumber * 4);
				long temp1 = 0xFFFFFFFF & (disk[(int)fatIndex +3] << 24);
				long temp2 = 0xFFFFFF & (disk[(int)fatIndex + 2] << 16);
				long temp3 = 0xFFFF & (disk[(int)fatIndex + 1] << 8);
				long temp4 = 0xFF & (disk[(int)fatIndex + 0]);
				clusterNumber = temp1 ^ temp2 ^ temp3 ^ temp4;
				pwd = (int)rootAddress + (int)clusterNumber*512;
			}
			
		} while (str.toString().trim().length() > 0);
		
		if(!found) {
			System.out.println("Error: does not exist");
			return pwdInit;
		}
		return pwdInit;
	}
	
	public static void read(byte[] disk, long pwd, int offset, int amount, long fatAddress, int rootAddress, String file) {
		StringBuilder str = new StringBuilder();
		boolean found = false;
		for(int i = 0; i < 11; i++) {
			str.append((char)disk[(int)pwd + i]);
		}
		do {
			for(int j = 0; j < 11; j++) {
				str.setCharAt(j, (char)disk[(int)pwd + j]);
			}
			if(!str.toString().substring(8,9).equals(" ")){
				str.insert(8, '.');
			}
			else{ 
				str.insert(8, " ");
			}
			String fileName = str.toString().replaceAll(" ", "");
			
			if(fileName.equals(file)) {
				found = true;
				byte attribute_byte = disk[(int)pwd + 11];
				if((attribute_byte & 0x10) == 0) {
					long clusterHigh = 0xFFFF & ((disk[(int)pwd + 21] << 8) ^ (disk[(int)pwd + 20]));
					long clusterLow  = 0xFFFF & ((disk[(int)pwd + 27] << 8) ^ (disk[(int)pwd + 26]));
					long clusterNumber = 0xFFFFFFFF &((clusterHigh << 16) ^ clusterLow);
					int beginningOfFile = (int)rootAddress + (int)clusterNumber*512;
					
					StringBuilder str2 = new StringBuilder();
					
					while(offset > 512) {
						int fatIndex = (int)fatAddress + (int)(clusterNumber * 4);
						long temp1 = 0xFFFFFFFF & (disk[(int)fatIndex +3] << 24);
						long temp2 = 0xFFFFFF & (disk[(int)fatIndex + 2] << 16);
						long temp3 = 0xFFFF & (disk[(int)fatIndex + 1] << 8);
						long temp4 = 0xFF & (disk[(int)fatIndex + 0]);
						clusterNumber = temp1 ^ temp2 ^ temp3 ^ temp4;
						
						offset -= 512;
					}
					
					beginningOfFile = (int)rootAddress + (int)clusterNumber*512;
					
					for(int counter = offset; counter < amount + offset; counter++) {
						
						if (counter % 512 == 0) {
							
							int fatIndex = (int)fatAddress + (int)(clusterNumber * 4);
							long temp1 = 0xFFFFFFFF & (disk[(int)fatIndex +3] << 24);
							long temp2 = 0xFFFFFF & (disk[(int)fatIndex + 2] << 16);
							long temp3 = 0xFFFF & (disk[(int)fatIndex + 1] << 8);
							long temp4 = 0xFF & (disk[(int)fatIndex + 0]);
							clusterNumber = temp1 ^ temp2 ^ temp3 ^ temp4;
							beginningOfFile = (int)rootAddress + (int)clusterNumber*512;
							
							counter -= 512;
							amount -= 512;
						}
						str2.append((char)disk[beginningOfFile + counter]);
					}
					System.out.println(str2.toString());
				} else {System.out.println("Error: cannot read directories");}
				
			}
			
			str.deleteCharAt(8);
			pwd += 64;
		} while (str.toString().trim().length() > 0);
		
		if(!found) {
			System.out.println("Error: file not preset");
		}
		
	}
	public static void volume(byte[] disk) {
		StringBuilder str = new StringBuilder();
		for(int i = 0; i < 11; i++) {
			str.append((char)disk[71 + i]);
		}
		System.out.println(str.toString());
	}

	
	public static int getNumberOfFreeClusters(byte[] disk, long fatAddress, long beginningOfData){
		int counter = 0;
		
		for(int i = 4; i < 16144; i++) {
			
			int byteNum = i*4;
			
			long temp1 = 0xFFFFFFFF & (disk[(int)fatAddress + byteNum + 3] << 24);
			long temp2 = 0xFFFFFF & (disk[(int)fatAddress + byteNum + 2] << 16);
			long temp3 = 0xFFFF & (disk[(int)fatAddress + byteNum + 1] << 8);
			long temp4 = 0xFF & (disk[(int)fatAddress + byteNum + 0]);
			long clusterNumber = temp1 ^ temp2 ^ temp3 ^ temp4;
			
			if(clusterNumber == 0) {
				counter++;
			}
		}
		
		return counter;
	}
	
	public static ArrayList<Long> getFreeClusters(byte[] disk, long fatAddress, long beginningOfData, int num){
		
		ArrayList<Long> addresses = new ArrayList<Long>();
		if(num == 0) return addresses;
		int counter = 0;
		
		for(int i = 4; i < 16144; i++) {
			
			int byteNum = i*4;
			
			long temp1 = 0xFFFFFFFF & (disk[(int)fatAddress + byteNum + 3] << 24);
			long temp2 = 0xFFFFFF & (disk[(int)fatAddress + byteNum + 2] << 16);
			long temp3 = 0xFFFF & (disk[(int)fatAddress + byteNum + 1] << 8);
			long temp4 = 0xFF & (disk[(int)fatAddress + byteNum + 0]);
			long clusterNumber = temp1 ^ temp2 ^ temp3 ^ temp4;
			
			if(clusterNumber == 0) {
				long address = beginningOfData + i*512;
				addresses.add(address);
				counter++;
				if(counter == num) {
					break;
				}
			}
		}
		return addresses;
	}
	
}

