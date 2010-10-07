package j2arduino.util;

/** Similar to Vector, but stores bytes only. */
public class ByteVector{
private static final int DEF_CAP = 128;
private byte[] array;
private int curPos;
private int inc;

/** Constructs an empty vector. If space runs out the buffer size will be doubled. */
public ByteVector(){
	this(DEF_CAP);
}

/**
 Constructs an empty vector with the specified initial capacity. If space runs out the buffer size will be doubled.

 @param capacity - initial capacity of new buffer. */
public ByteVector(int capacity){
	this(capacity, -1);
}

/**
 Constructs an empty vector with the specified initial capacity and capacity increment.

 @param initialCapacity   initial capacity of new buffer.
 @param capacityIncrement the amount by which the capacity is increased when the vector overflows. */
public ByteVector(int initialCapacity, int capacityIncrement){
	array = new byte[initialCapacity];
	curPos = 0;
	inc = capacityIncrement;
}

/**
 Appends a byte to this buffer.

 @param b byte to be appended.
 @return this ByteVector */
public ByteVector append(byte b){
	ensureCapacity(curPos + 1);
	array[curPos++] = b;
	return this;
}

/**
 Appends an array to this buffer.

 @param bytes bytes to be appended.
 @return this ByteVector */
public ByteVector append(byte[] bytes){
	return append(bytes, 0, bytes.length);
}

/**
 Appends a part of an array.

 Appends the contents of array bytes to this array, starting and including offset 'offset'.

 @param bytes  the array to be appended
 @param offset the offset of bytes used
 @return this ByteVector */
public ByteVector append(byte[] bytes, int offset){
	return append(bytes, offset, bytes.length - offset);
}

/**
 Appends a part of an array.

 Appends the contents of array bytes to this array, starting and including offset \a offset ending after \a length elements.

 @param bytes  the array to be appended
 @param offset the offset of bytes used
 @param length how many byte should be appended
 @return this ByteVector */
public ByteVector append(byte[] bytes, int offset, int length){
	ensureCapacity(curPos + length);
	System.arraycopy(bytes, offset, array, curPos, length);
	curPos += length;
	return this;
}

/**
 Returns the amount of bytes in this buffer.

 @return number of bytes in buffer. */
public int length(){
	return curPos;
}

/**
 Returns the byte at the specified index in this buffer.

 @param index index of byte to be returned.
 @return byte at specified position. */
public byte get(int index){
	return array[index];
}

/**
 Returns all bytes in this buffer.

 The returned array contains copies of the bytes previously added to this buffer.

 @return an array of bytes in this buffer. */
public byte[] getAll(){
	byte[] res = new byte[curPos];
	System.arraycopy(array, 0, res, 0, curPos);
	return res;
}

/**
 Ensures, that the storing array can hold at least \a minimumCapacity bytes.

 Allocates a new array (\a inc elements larger or double the current size if \a inc is not set) and copies the content of the old array over to the
 new one.

 @param minimumCapacity the minimum amount of elements to store afterwards */
private void ensureCapacity(int minimumCapacity){
	if(minimumCapacity <= array.length){
		return;
	}

	int newLength;
	if(inc > 0){
		newLength = array.length + inc;
	} else{
		newLength = array.length<<1;
	}
	if(minimumCapacity > newLength){
		newLength = minimumCapacity;
	}
	byte[] newArray = new byte[newLength];
	System.arraycopy(array, 0, newArray, 0, curPos);
	array = newArray;
}
}