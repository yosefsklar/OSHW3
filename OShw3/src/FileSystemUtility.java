import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

public class FileSystemUtility {

	public static void main(String[] args) throws IOException {
		
		long BPB_BytesPerSec;
		long BPB_SecPerClus;
		long BPB_RsvdSecCnt;
		long BPB_NumFATS;
		long BPB_FATSz32;
		long pwd_address;
		boolean running = true;
		
		
		
		Path diskPath = Paths.get("C:\\Users\\Avi\\Desktop\\OS-project-3\\OSHW3", "fat32.img");
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
		pwd_address = FirstSectorOfCluster;
		//System.out.println(FirstDataSector);
		//System.out.println(FirstSectorOfCluster);
		//System.out.println(diskImage[(int)FirstSectorOfCluster]);
		
		//ls(diskImage, FirstSectorOfCluster);
		//stat(diskImage, pwd_address, "FSINFO.TXT");
		
		while(running) {
			System.out.print("> ");
			Scanner scan = new Scanner(System.in);
			String input = scan.nextLine();
			String[] arr = input.split(" ");
			String command = arr[0];
			
			if(command.equals("info")) {
				info(BPB_BytesPerSec, BPB_SecPerClus, BPB_RsvdSecCnt, BPB_NumFATS, BPB_FATSz32);
			} else if(command.equals("stat")) {
				String file = arr[1];
				stat(diskImage, pwd_address, file);
			} else if (command.equals("ls")) {
				ls(diskImage, pwd_address);
			} else if(command.equals("exit")) {
				running = false;
			}
		}
		
		
		//System.out.println(BPB_BytesPerSec);
		//System.out.println(BPB_SecPerClus);
		//System.out.println(BPB_RsvdSecCnt);
		//System.out.println(BPB_NumFATS);
		//System.out.println(BPB_FATSz32);
		
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
	
	public static void stat(byte[] disk, long root, String entry) {
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
					str2.append("ATTR_IRECTORY ");
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
		}while(str.toString().trim().length() > 0);	
		if(!foundEntry){
			System.out.println("Could not find " + entry);
		}
	}
	
	public static void ls(byte[] disk, long root) {
	
		StringBuilder str = new StringBuilder();
		for(int i = 0; i < 11; i++) {
			str.append((char)disk[(int)root + i]);
		}
		while(str.toString().trim().length() > 0){
			System.out.println(str);
			root += 32;
			for(int j = 0; j < 11; j++) {
				str.setCharAt(j, (char)disk[(int)root + j]);
			}
		}	
		
	
	}
}
