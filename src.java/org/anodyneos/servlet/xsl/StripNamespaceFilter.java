package org.anodyneos.servlet.xsl;

import java.util.HashSet;
import java.util.Set;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 * @author jvas
 *
 * All elements and attributes for any of the specified namespace URIs will be
 * stripped of their namespace and prefix. The default namespace for the output
 * will always be "" (empty) for elements of the namespace(s) to be stripped.
 * If the input document attempts to map a default namespace that does not
 * match one of the specified URIs, a prefix will be automatically generated
 * for that namespace and all elements will be modified to include that prefix.
 *
 * Warning: this class is not thread safe and should only be used in one filter
 * chain at a time.
 *
 * Warning: this class relies upon the parent to correctly call
 * startNamespaceMapping() and endNamespaceMapping() per the SAX specification.
 * Xalan does not do this for SAXResults when the output property is set to
 * <code>html</code>.
 *
 * BUGS: Prefix translation does not occur if the input tries to create a
 * prefix mapping for a prefix that has already been created to map a default
 * prefix.
 */
public class StripNamespaceFilter extends XMLFilterImpl {

    private NamespaceMapping mappings;

    private NamespaceMapping defaultNSPrefixes;

    private Set namespaces = new HashSet();

    private int bigNum = (int) Math.pow(36, 3);

    /**
     * Create a new instance; if setNamespaces() is not called, the new
     * instance will strip the namespace <code>http://www.w3.org/1999/xhtml</code>.
     */
    public StripNamespaceFilter() {
        super();
        this.namespaces.add("http://www.w3.org/1999/xhtml");
    }

    /**
     * Create a new instance; if setNamespaces() is not called, the new
     * instance will strip the namespace <code>http://www.w3.org/1999/xhtml</code>.
     */
    public StripNamespaceFilter(XMLReader parent) {
        super(parent);
        this.namespaces.add("http://www.w3.org/1999/xhtml");
    }

    /**
     * Sets the namespaces this filter should strip. Calling this method will
     * override the default setting.
     *
     * @param namespaces
     *            the namespaces this filter should strip.
     */
    public void setNamespaces(String[] namespaces) {
        for (int i = 0; i < namespaces.length; i++) {
            this.namespaces.clear();
            this.namespaces.add(namespaces[i]);
        }
    }

    public void startDocument() throws SAXException {
        mappings = new NamespaceMapping();
        defaultNSPrefixes = new NamespaceMapping();
        super.startDocument();
    }

    public void endDocument() throws SAXException {
        mappings = null;
        defaultNSPrefixes = null;
        super.endDocument();
    }

    private Attributes cleanAttributes(Attributes attrs) {
        AttributesImpl newAttrs = null;
        for (int i = 0; i < attrs.getLength(); i++) {
            if (namespaces.contains(attrs.getURI(i))) {
                if (null == newAttrs) {
                    // catch up
                    newAttrs = new AttributesImpl();
                    for (int j = 0; j < i; j++) {
                        newAttrs.addAttribute(attrs.getURI(j), attrs.getLocalName(j), attrs
                                .getQName(j), attrs.getType(j), attrs.getValue(j));
                    }
                }
                newAttrs.addAttribute("", attrs.getLocalName(i), attrs.getLocalName(i), attrs
                        .getType(i), attrs.getValue(i));
            } else if (null != newAttrs) {
                // keep up
                newAttrs.addAttribute(attrs.getURI(i), attrs.getLocalName(i), attrs.getQName(i),
                        attrs.getType(i), attrs.getValue(i));
            }
        }

        if (newAttrs != null) {
            return newAttrs;
        } else {
            return attrs;
        }
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {
        /*
         * Don't trust uri, Xalan leaves this out sometimes for startElement
         * and sometimes for endElement. Use prefixMapping instead.
         *
         * If uri is in namespaces call super.endElement with "", localName,
         * localName
         *
         * If uri not in namespaces and prefix == "" modify qName to reflect
         * made up prefix in defaultNSPrefixes
         *
         * If uri not in namepsaces and prefix != "" call super.endElement with
         * uri, localName, qName
         *
         */
        String prefix = parsePrefix(qName);
        String myURI = mappings.peek(prefix);

        if (namespaces.contains(myURI)) {
            super.endElement("", localName, localName);
        } else { // ! namespace.contains(uri)
            if (null == prefix || prefix.length() == 0) {
                String newQName = defaultNSPrefixes.findPrefixForURI(myURI) + ":" + localName;
                super.endElement(myURI, localName, newQName);
            } else { // prefix not empty
                super.endElement(uri, localName, qName);
            }
        }
    }

    public void endPrefixMapping(String prefix) throws SAXException {
        /*
         * Always pop from mappings after done processing
         *
         * If uri is in namespaces Don't call super.endPrefixMapping
         *
         * If uri not in namespaces and prefix == "" Find made up prefix in
         * defaultNSPrefixes and pop. If no more entries exist for this prefix
         * call super.endPrefixMapping with this prefix If more entries exist
         * for this prefix Do nothing more.
         *
         * If uri not in namespaces and prefix != "" Call
         * super.endPrefixMappings
         */
        String uri = mappings.peek(prefix);
        if (namespaces.contains(uri)) {
            // do nothing
        } else {
            if (null == prefix || prefix.length() == 0) {
                String myPrefix = defaultNSPrefixes.findPrefixForURI(uri);
                defaultNSPrefixes.pop(myPrefix);
                // end mapping for this prefix if we no longer have references.
                if (!defaultNSPrefixes.prefixExists(myPrefix)) {
                    super.endPrefixMapping(myPrefix);
                }
            } else { // prefix is not empty
                super.endPrefixMapping(prefix);
            }
        }
        mappings.pop(prefix);
    }

    public void startElement(String uri, String localName, String qName, Attributes attrs)
            throws SAXException {
        /*
         * Don't trust uri, Xalan leaves this out sometimes for startElement
         * and sometimes for endElement. Use prefixMapping instead.
         *
         * If uri is in namespace or no namespace for element call startElement
         * with "", localName, localName, clean(attrs)
         *
         * else prefix == "" modify qName to reflect made up prefix in
         * defaultNSPrefixes
         *
         * else prefix != "" call startElement with uri, localName, qName,
         * clean(attrs)
         *
         */
        Attributes newAttrs = cleanAttributes(attrs);
        String prefix = parsePrefix(qName);
        String myURI = mappings.peek(prefix);

        if ("".equals(myURI) || namespaces.contains(myURI)) {
            super.startElement("", localName, localName, newAttrs);
        } else { // ! namespace.contains(uri)
            if (null == prefix || prefix.length() == 0) {
                String newQName = defaultNSPrefixes.findPrefixForURI(myURI) + ":" + localName;
                super.startElement(myURI, localName, newQName, newAttrs);
            } else { // prefix not empty
                super.startElement(uri, localName, qName, newAttrs);
            }
        }
    }

    public void startPrefixMapping(String prefix, String uri) throws SAXException {
        /*
         * Store mapping in mappings
         *
         * If prefix == "" and uri not in namespaces, Search for pre-existing
         * prefix in defaultNSMapping for uri If exists, add another entry for
         * this prefix to the stack in defaultNSPrefixes If doesn't exist
         * create one store in defaultNSPrefixes call super.startPrefixMapping
         * for this new prefix and uri
         *
         * If prefix == "" and uri is in namespaces, Don't call
         * super.startPrefixMapping
         *
         * If prefix != "" and uri is in namespaces, Don't call
         * super.startPrefixMapping (output default NS is always "")
         *
         * If prefix != "" and uri not in namespaces, Call
         * super.startPrefixMapping.
         */
        mappings.push(prefix, uri);
        if (namespaces.contains(uri)) {
            // do nothing.
        } else { // ! namespaces.contains(uri)
            if (null == prefix || prefix.length() == 0) {
                String myPrefix = defaultNSPrefixes.findPrefixForURI(uri);
                if (null != myPrefix) {
                    // add another "reference" for when endPrefixMapping is
                    // called
                    defaultNSPrefixes.push(myPrefix, uri);
                } else {
                    // create new prefix
                    myPrefix = genPrefix();
                    defaultNSPrefixes.push(myPrefix, uri);
                    super.startPrefixMapping(myPrefix, uri);
                }
            } else { // prefix is not empty
                // we really should check for prefix conflicts with the ones in
                // defaultNSPrefixes
                super.startPrefixMapping(prefix, uri);
            }
        }
    }

    private String genPrefix() {
        /*
         * Problem: The same URI may be mapped as a default namespace multiple
         * times with other URIs in between. We really only care about the most
         * recent default namespace mapping but we need to be able to search
         * the prefix names.
         *
         * We don't need to create a new prefix for a default NS if one already
         * exists, but we need to make sure not to prematurely remove our
         * mapping. So, basically we can keep a reference count by adding the
         * same URI to the prefix as often as we need to.
         */
        String prefix;
        do {
            // comment this out. Better to be repeatable.
            //prefix = "n" + Integer.toString((int) (Math.random() *
            // Integer.MAX_VALUE), 36);
            prefix = "n" + Integer.toString(bigNum++, 36);
        } while (mappings.prefixExists(prefix) || defaultNSPrefixes.prefixExists(prefix));
        return prefix;
    }

    private String parsePrefix(String qName) {
        if (null == qName || qName.length() == 0) {
            return "";
        } else {
            int colon = qName.indexOf(':');
            if (-1 == colon) {
                return "";
            } else {
                return qName.substring(0, colon);
            }
        }
    }
}