package com.tcl.smartapp.fileutils;

import java.util.ArrayList;

class Queqe<T>
{
	ArrayList<T> arrayList = new ArrayList<T>();

	public void enQueqe(T t)
	{
		arrayList.add(t);
	}

	public T getHead()
	{
		if (isEmpty())
			return null;
		return arrayList.get(0);
	}

	public T deQueqe()
	{
		if (isEmpty())
			return null;
		return arrayList.remove(0);

	}

	public boolean isEmpty()
	{
		return arrayList.size() == 0 ? true : false;
	}

}