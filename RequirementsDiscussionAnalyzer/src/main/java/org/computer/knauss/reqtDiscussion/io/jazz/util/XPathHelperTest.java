package org.computer.knauss.reqtDiscussion.io.jazz.util;

import static org.junit.Assert.assertEquals;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.jdom2.Attribute;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

public class XPathHelperTest {

	private static final String DOORS_GENERAL_EXAMPLE_TESTFILE_PATH = "testfiles/jazz.xml/general_test.xml";

	private static final String BOOK_EXAMPLE_TESTFILE_PATH = "testfiles/jazz.xml/buchbsp.xml";

	private XPathHelper helper;

	@Before
	public void setup() {
		helper = new XPathHelper();
	}

	@Test
	public void testDocument() throws SAXException, IOException,
			ParserConfigurationException, JDOMException {
		helper.setDocument(new FileInputStream(BOOK_EXAMPLE_TESTFILE_PATH));
		org.jdom2.Document d = helper.getDocument();
		assertEquals("inventory", d.getRootElement().getName());
	}

	@Test
	public void testBuchBsp() throws XPathExpressionException,
			ParserConfigurationException, SAXException, IOException,
			JDOMException {
		helper.setDocument(new FileInputStream(BOOK_EXAMPLE_TESTFILE_PATH));
		String xmlExp = "//book[author='Neal Stephenson']/title/text()";
		// helper.resolve(xmlExp);
		assertEquals(2, helper.count(xmlExp));
		assertEquals(3, helper.count("//book"));
	}

	@Test
	public void testDaimlerBsp() throws XPathExpressionException,
			ParserConfigurationException, SAXException, IOException,
			JDOMException {
		helper.setDocument(new FileInputStream(
				DOORS_GENERAL_EXAMPLE_TESTFILE_PATH));
		assertEquals(1, helper.count("/doorsexport"));
		assertEquals(1,
				helper.count("//object[column/heading/span='Eingänge']/table"));
		assertEquals(
				6,
				helper.count("//object[column/heading/span='Eingänge']/table/tr"));
		assertEquals(
				5,
				helper.count("//object[column/heading/span='Ausgänge']/table/tr"));
		assertEquals(
				3,
				helper.count("//object[column/heading/span='Parameter']/table/tr"));
		// helper.resolve("//object[column/heading/span='Eingänge']/table");
		// printRec(
		// helper.select("//object[column/heading/span='Eingänge']/table") );
		List<Object> list = helper
				.select("//object[column/heading/span='Eingänge']/table/tr");
		list = helper.select(list.get(0), ".//td");
		assertEquals(3, list.size());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testSelectWithNullNode() throws XPathExpressionException,
			SAXException, IOException, ParserConfigurationException, JDOMException {
		helper.setDocument(new FileInputStream(DOORS_GENERAL_EXAMPLE_TESTFILE_PATH));
		helper.select(null, "//");
	}

	
	@Test
	public void testGetResourcesFromRootservices() throws IOException, JDOMException {
		FileInputStream documentStream = new FileInputStream("testfiles/jazz.xml/rootservices.xml");
		helper.setDocument(documentStream);
		Attribute queryResource = (Attribute) helper.select("//query/@resource").get(0);
		assertEquals("https://jazz.net/jazz/query",queryResource.getValue());
		documentStream.close();
	}
	
	@Test
	public void testRelativeSelect() throws FileNotFoundException, JDOMException, IOException {
		helper.setDocument(new FileInputStream(BOOK_EXAMPLE_TESTFILE_PATH));
		List<Object> books = helper.select("//book");
		
		assertEquals(3, books.size());
		assertEquals("14.95", ((Element)helper.select(books.get(0), ".//price").get(0)).getValue());
		assertEquals("5.99", ((Element)helper.select(books.get(1), ".//price").get(0)).getValue());
		assertEquals("7.50", ((Element)helper.select(books.get(2), ".//price").get(0)).getValue());
	}
}