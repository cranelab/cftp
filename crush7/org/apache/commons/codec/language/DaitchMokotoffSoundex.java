package org.apache.commons.codec.language;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.StringEncoder;

public class DaitchMokotoffSoundex implements StringEncoder {
  private static final String COMMENT = "//";
  
  private static final String DOUBLE_QUOTE = "\"";
  
  private static final String MULTILINE_COMMENT_END = "*/";
  
  private static final String MULTILINE_COMMENT_START = "/*";
  
  private static final String RESOURCE_FILE = "org/apache/commons/codec/language/dmrules.txt";
  
  private static final int MAX_LENGTH = 6;
  
  private static final class Branch {
    private final StringBuilder builder = new StringBuilder();
    
    private String lastReplacement = null;
    
    private String cachedString = null;
    
    public Branch createBranch() {
      Branch branch = new Branch();
      branch.builder.append(toString());
      branch.lastReplacement = this.lastReplacement;
      return branch;
    }
    
    public boolean equals(Object other) {
      if (this == other)
        return true; 
      if (!(other instanceof Branch))
        return false; 
      return toString().equals(((Branch)other).toString());
    }
    
    public void finish() {
      while (this.builder.length() < 6) {
        this.builder.append('0');
        this.cachedString = null;
      } 
    }
    
    public int hashCode() {
      return toString().hashCode();
    }
    
    public void processNextReplacement(String replacement, boolean forceAppend) {
      boolean append = (this.lastReplacement == null || !this.lastReplacement.endsWith(replacement) || forceAppend);
      if (append && this.builder.length() < 6) {
        this.builder.append(replacement);
        if (this.builder.length() > 6)
          this.builder.delete(6, this.builder.length()); 
        this.cachedString = null;
      } 
      this.lastReplacement = replacement;
    }
    
    public String toString() {
      if (this.cachedString == null)
        this.cachedString = this.builder.toString(); 
      return this.cachedString;
    }
    
    private Branch() {}
  }
  
  private static final class Rule {
    private final String pattern;
    
    private final String[] replacementAtStart;
    
    private final String[] replacementBeforeVowel;
    
    private final String[] replacementDefault;
    
    protected Rule(String pattern, String replacementAtStart, String replacementBeforeVowel, String replacementDefault) {
      this.pattern = pattern;
      this.replacementAtStart = replacementAtStart.split("\\|");
      this.replacementBeforeVowel = replacementBeforeVowel.split("\\|");
      this.replacementDefault = replacementDefault.split("\\|");
    }
    
    public int getPatternLength() {
      return this.pattern.length();
    }
    
    public String[] getReplacements(String context, boolean atStart) {
      if (atStart)
        return this.replacementAtStart; 
      int nextIndex = getPatternLength();
      boolean nextCharIsVowel = (nextIndex < context.length()) ? isVowel(context.charAt(nextIndex)) : false;
      if (nextCharIsVowel)
        return this.replacementBeforeVowel; 
      return this.replacementDefault;
    }
    
    private boolean isVowel(char ch) {
      return (ch == 'a' || ch == 'e' || ch == 'i' || ch == 'o' || ch == 'u');
    }
    
    public boolean matches(String context) {
      return context.startsWith(this.pattern);
    }
    
    public String toString() {
      return String.format("%s=(%s,%s,%s)", new Object[] { this.pattern, Arrays.asList(this.replacementAtStart), Arrays.asList(this.replacementBeforeVowel), Arrays.asList(this.replacementDefault) });
    }
  }
  
  private static final Map<Character, List<Rule>> RULES = new HashMap<Character, List<Rule>>();
  
  private static final Map<Character, Character> FOLDINGS = new HashMap<Character, Character>();
  
  private final boolean folding;
  
  static {
    InputStream rulesIS = DaitchMokotoffSoundex.class.getClassLoader().getResourceAsStream("org/apache/commons/codec/language/dmrules.txt");
    if (rulesIS == null)
      throw new IllegalArgumentException("Unable to load resource: org/apache/commons/codec/language/dmrules.txt"); 
    Scanner scanner = new Scanner(rulesIS, "UTF-8");
    parseRules(scanner, "org/apache/commons/codec/language/dmrules.txt", RULES, FOLDINGS);
    scanner.close();
    for (Map.Entry<Character, List<Rule>> rule : RULES.entrySet()) {
      List<Rule> ruleList = rule.getValue();
      Collections.sort(ruleList, new Comparator<Rule>() {
            public int compare(DaitchMokotoffSoundex.Rule rule1, DaitchMokotoffSoundex.Rule rule2) {
              return rule2.getPatternLength() - rule1.getPatternLength();
            }
          });
    } 
  }
  
  private static void parseRules(Scanner scanner, String location, Map<Character, List<Rule>> ruleMapping, Map<Character, Character> asciiFoldings) {
    int currentLine = 0;
    boolean inMultilineComment = false;
    while (scanner.hasNextLine()) {
      currentLine++;
      String rawLine = scanner.nextLine();
      String line = rawLine;
      if (inMultilineComment) {
        if (line.endsWith("*/"))
          inMultilineComment = false; 
        continue;
      } 
      if (line.startsWith("/*")) {
        inMultilineComment = true;
        continue;
      } 
      int cmtI = line.indexOf("//");
      if (cmtI >= 0)
        line = line.substring(0, cmtI); 
      line = line.trim();
      if (line.length() == 0)
        continue; 
      if (line.contains("=")) {
        String[] arrayOfString = line.split("=");
        if (arrayOfString.length != 2)
          throw new IllegalArgumentException("Malformed folding statement split into " + arrayOfString.length + " parts: " + rawLine + " in " + location); 
        String leftCharacter = arrayOfString[0];
        String rightCharacter = arrayOfString[1];
        if (leftCharacter.length() != 1 || rightCharacter.length() != 1)
          throw new IllegalArgumentException("Malformed folding statement - patterns are not single characters: " + rawLine + " in " + location); 
        asciiFoldings.put(Character.valueOf(leftCharacter.charAt(0)), Character.valueOf(rightCharacter.charAt(0)));
        continue;
      } 
      String[] parts = line.split("\\s+");
      if (parts.length != 4)
        throw new IllegalArgumentException("Malformed rule statement split into " + parts.length + " parts: " + rawLine + " in " + location); 
      try {
        String pattern = stripQuotes(parts[0]);
        String replacement1 = stripQuotes(parts[1]);
        String replacement2 = stripQuotes(parts[2]);
        String replacement3 = stripQuotes(parts[3]);
        Rule r = new Rule(pattern, replacement1, replacement2, replacement3);
        char patternKey = r.pattern.charAt(0);
        List<Rule> rules = ruleMapping.get(Character.valueOf(patternKey));
        if (rules == null) {
          rules = new ArrayList<Rule>();
          ruleMapping.put(Character.valueOf(patternKey), rules);
        } 
        rules.add(r);
      } catch (IllegalArgumentException e) {
        throw new IllegalStateException("Problem parsing line '" + currentLine + "' in " + location, e);
      } 
    } 
  }
  
  private static String stripQuotes(String str) {
    if (str.startsWith("\""))
      str = str.substring(1); 
    if (str.endsWith("\""))
      str = str.substring(0, str.length() - 1); 
    return str;
  }
  
  public DaitchMokotoffSoundex() {
    this(true);
  }
  
  public DaitchMokotoffSoundex(boolean folding) {
    this.folding = folding;
  }
  
  private String cleanup(String input) {
    StringBuilder sb = new StringBuilder();
    for (char ch : input.toCharArray()) {
      if (!Character.isWhitespace(ch)) {
        ch = Character.toLowerCase(ch);
        if (this.folding && FOLDINGS.containsKey(Character.valueOf(ch)))
          ch = ((Character)FOLDINGS.get(Character.valueOf(ch))).charValue(); 
        sb.append(ch);
      } 
    } 
    return sb.toString();
  }
  
  public Object encode(Object obj) throws EncoderException {
    if (!(obj instanceof String))
      throw new EncoderException("Parameter supplied to DaitchMokotoffSoundex encode is not of type java.lang.String"); 
    return encode((String)obj);
  }
  
  public String encode(String source) {
    if (source == null)
      return null; 
    return soundex(source, false)[0];
  }
  
  public String soundex(String source) {
    String[] branches = soundex(source, true);
    StringBuilder sb = new StringBuilder();
    int index = 0;
    for (String branch : branches) {
      sb.append(branch);
      if (++index < branches.length)
        sb.append('|'); 
    } 
    return sb.toString();
  }
  
  private String[] soundex(String source, boolean branching) {
    if (source == null)
      return null; 
    String input = cleanup(source);
    Set<Branch> currentBranches = new LinkedHashSet<Branch>();
    currentBranches.add(new Branch());
    char lastChar = Character.MIN_VALUE;
    for (int index = 0; index < input.length(); index++) {
      char ch = input.charAt(index);
      if (!Character.isWhitespace(ch)) {
        String inputContext = input.substring(index);
        List<Rule> rules = RULES.get(Character.valueOf(ch));
        if (rules != null) {
          List<Branch> nextBranches = branching ? new ArrayList<Branch>() : Collections.EMPTY_LIST;
          for (Rule rule : rules) {
            if (rule.matches(inputContext)) {
              if (branching)
                nextBranches.clear(); 
              String[] replacements = rule.getReplacements(inputContext, (lastChar == '\000'));
              boolean branchingRequired = (replacements.length > 1 && branching);
              for (Branch branch : currentBranches) {
                String[] arr$;
                int len$;
                int i$;
                for (arr$ = replacements, len$ = arr$.length, i$ = 0; i$ < len$; ) {
                  String nextReplacement = arr$[i$];
                  Branch nextBranch = branchingRequired ? branch.createBranch() : branch;
                  boolean force = ((lastChar == 'm' && ch == 'n') || (lastChar == 'n' && ch == 'm'));
                  nextBranch.processNextReplacement(nextReplacement, force);
                  if (branching) {
                    nextBranches.add(nextBranch);
                    i$++;
                  } 
                } 
              } 
              if (branching) {
                currentBranches.clear();
                currentBranches.addAll(nextBranches);
              } 
              index += rule.getPatternLength() - 1;
              break;
            } 
          } 
          lastChar = ch;
        } 
      } 
    } 
    String[] result = new String[currentBranches.size()];
    int i = 0;
    for (Branch branch : currentBranches) {
      branch.finish();
      result[i++] = branch.toString();
    } 
    return result;
  }
}
